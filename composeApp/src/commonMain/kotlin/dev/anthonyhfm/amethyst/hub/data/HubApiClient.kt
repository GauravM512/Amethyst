package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class HubApiException(
    val errorCode: String,
    val statusCode: Int,
    val retryAfterSeconds: Long? = null,
) : Exception("Amethyst Hub request failed ($statusCode): $errorCode")

class HubApiClient internal constructor(
    baseUrl: String,
    bearerToken: String?,
    refreshToken: String?,
    private val onSessionChanged: ((HubSessionTokens?) -> Unit)?,
    internal val http: HttpClient,
) {
    constructor(
        baseUrl: String = DEFAULT_BASE_URL,
        bearerToken: String? = null,
        refreshToken: String? = null,
        onSessionChanged: ((HubSessionTokens?) -> Unit)? = null,
    ) : this(baseUrl, bearerToken, refreshToken, onSessionChanged, createHubHttpClient())

    internal val baseUrl = baseUrl.trimEnd('/')
    private val refreshMutex = Mutex()
    private var accessTokenIssuedAt: TimeMark? = null
    private var accessTokenLifetimeSeconds: Long? = null

    var bearerToken: String? = bearerToken
        set(value) {
            field = value
            accessTokenIssuedAt = null
            accessTokenLifetimeSeconds = null
        }

    var refreshToken: String? = refreshToken

    val isAuthenticated: Boolean get() = bearerToken != null && refreshToken != null

    fun restoreSession(tokens: HubSessionTokens) {
        applySession(tokens, notify = false)
    }

    fun clearSession() {
        bearerToken = null
        refreshToken = null
        onSessionChanged?.invoke(null)
    }

    fun resolveUrl(pathOrUrl: String): String = when {
        pathOrUrl.startsWith("https://") || pathOrUrl.startsWith("http://") -> pathOrUrl
        pathOrUrl.startsWith("/api/projects/") -> "$baseUrl${pathOrUrl.removePrefix("/api")}"
        pathOrUrl.startsWith('/') -> "$baseUrl$pathOrUrl"
        else -> "$baseUrl/$pathOrUrl"
    }

    internal fun acceptAuthentication(result: HubAuthResult) {
        val access = result.accessToken ?: return
        val refresh = result.refreshToken ?: return
        applySession(HubSessionTokens(access, refresh, result.expiresIn ?: DEFAULT_ACCESS_TOKEN_TTL), notify = true)
    }

    suspend fun refreshSession(): HubAuthResult {
        val availableRefreshToken = refreshToken
            ?: throw HubApiException("authentication_required", HttpStatusCode.Unauthorized.value)
        return refreshMutex.withLock { requestRefresh(refreshToken ?: availableRefreshToken) }
    }

    private fun applySession(tokens: HubSessionTokens, notify: Boolean) {
        bearerToken = tokens.accessToken
        refreshToken = tokens.refreshToken
        accessTokenIssuedAt = TimeSource.Monotonic.markNow()
        accessTokenLifetimeSeconds = tokens.expiresIn
        if (notify) onSessionChanged?.invoke(tokens)
    }

    private fun accessTokenNearExpiry(): Boolean {
        val issuedAt = accessTokenIssuedAt ?: return false
        val lifetime = accessTokenLifetimeSeconds ?: return false
        return issuedAt.elapsedNow() >= (lifetime - REFRESH_SKEW_SECONDS).coerceAtLeast(0).seconds
    }

    private suspend fun freshAccessToken(force: Boolean, failedToken: String? = null): String? {
        val current = bearerToken
        if (!force && !accessTokenNearExpiry()) return current
        val availableRefreshToken = refreshToken ?: return current

        return refreshMutex.withLock {
            if (force && failedToken != null && bearerToken != failedToken) return@withLock bearerToken
            if (!force && !accessTokenNearExpiry()) return@withLock bearerToken

            requestRefresh(refreshToken ?: availableRefreshToken)
            bearerToken
        }
    }

    private suspend fun requestRefresh(token: String): HubAuthResult {
        try {
            val result = http.post("$baseUrl/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(HubRefreshInput(token))
            }.hubBody<HubAuthResult>()
            acceptAuthentication(result)
            return result
        } catch (error: HubApiException) {
            if (error.statusCode == HttpStatusCode.Unauthorized.value) clearSession()
            throw error
        }
    }

    internal suspend fun authorized(request: suspend (String) -> HttpResponse): HttpResponse {
        val token = freshAccessToken(force = false)
            ?: throw HubApiException("authentication_required", HttpStatusCode.Unauthorized.value)
        val response = request(token)
        if (response.status != HttpStatusCode.Unauthorized || refreshToken == null) return response
        val refreshed = freshAccessToken(force = true, failedToken = token)
            ?: throw HubApiException("authentication_required", HttpStatusCode.Unauthorized.value)
        return request(refreshed)
    }

    internal suspend fun optionallyAuthorized(request: suspend (String?) -> HttpResponse): HttpResponse {
        val token = if (bearerToken != null) freshAccessToken(force = false) else null
        return request(token)
    }

    fun close() = http.close()

    companion object {
        const val DEFAULT_BASE_URL = "https://api.anthonyhfm.dev"
        private const val DEFAULT_ACCESS_TOKEN_TTL = 300L
        private const val REFRESH_SKEW_SECONDS = 30L
    }
}

private fun createHubHttpClient() = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        })
    }
}

internal fun HttpRequestBuilder.applyBearerAuth(token: String?) {
    if (token != null) headers.append(HttpHeaders.Authorization, "Bearer $token")
}

internal suspend inline fun <reified T> HttpResponse.hubBody(): T {
    if (status.isSuccess()) return body()
    val error = runCatching { body<HubErrorResponse>().error }.getOrNull() ?: "http_${status.value}"
    throw HubApiException(
        errorCode = error,
        statusCode = status.value,
        retryAfterSeconds = headers[HttpHeaders.RetryAfter]?.toLongOrNull(),
    )
}
