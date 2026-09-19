package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class HubApiClient(
    baseUrl: String = DEFAULT_BASE_URL,
    var bearerToken: String? = null,
) {

    internal val baseUrl = baseUrl.trimEnd('/')

    internal val http: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    fun close() = http.close()

    companion object {
        const val DEFAULT_BASE_URL = "https://api.amethyst.anthonyhfm.dev"
    }
}

internal fun HttpRequestBuilder.applyBearerAuth(token: String?) {
    if (token != null) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
