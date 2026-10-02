package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HubApiClientTest {
    @Test
    fun loginStoresNativeTokensAndReportsSessionChanges() = runTest {
        var changed: HubSessionTokens? = null
        val client = client(
            handler = {
                assertEquals("/v1/auth/login", it.url.encodedPath)
                respond(
                    """{"accessToken":"access","refreshToken":"refresh","expiresIn":300,"account":${accountJson()}}""",
                    headers = jsonHeaders,
                )
            },
            onSessionChanged = { changed = it },
        )

        val result = LoginUseCase(client).execute(HubLoginInput("artist", "password"))

        assertTrue(result.isAuthenticated)
        assertTrue(client.isAuthenticated)
        assertEquals("access", client.bearerToken)
        assertEquals("refresh", changed?.refreshToken)
        client.close()
    }

    @Test
    fun authorizedRequestRefreshesOnceAfterUnauthorized() = runTest {
        var accountRequests = 0
        var refreshRequests = 0
        val client = client(
            bearerToken = "expired",
            refreshToken = "refresh-1",
            handler = {
                when (it.url.encodedPath) {
                    "/v1/account" -> {
                        accountRequests++
                        if (it.headers[HttpHeaders.Authorization] == "Bearer expired") {
                            respond("""{"error":"unauthorized"}""", HttpStatusCode.Unauthorized, jsonHeaders)
                        } else {
                            assertEquals("Bearer access-2", it.headers[HttpHeaders.Authorization])
                            respond(accountJson(), headers = jsonHeaders)
                        }
                    }

                    "/v1/auth/refresh" -> {
                        refreshRequests++
                        respond(
                            """{"accessToken":"access-2","refreshToken":"refresh-2","expiresIn":300,"account":${accountJson()}}""",
                            headers = jsonHeaders,
                        )
                    }

                    else -> error("Unexpected request to ${it.url.encodedPath}")
                }
            },
        )

        val account = GetAccountUseCase(client).execute()

        assertEquals("artist", account.username)
        assertEquals(2, accountRequests)
        assertEquals(1, refreshRequests)
        assertEquals("refresh-2", client.refreshToken)
        client.close()
    }

    @Test
    fun searchUsesQueryLimitAndOptionalBearerToken() = runTest {
        val client = client(
            bearerToken = "access",
            refreshToken = "refresh",
            handler = {
                assertEquals("/search", it.url.encodedPath)
                assertEquals("launch pad", it.url.parameters["q"])
                assertEquals("7", it.url.parameters["limit"])
                assertEquals("Bearer access", it.headers[HttpHeaders.Authorization])
                respond(
                    """{"query":"launch pad","projects":[],"artists":[],"projectCount":0,"artistCount":0}""",
                    headers = jsonHeaders,
                )
            },
        )

        val result = SearchHubUseCase(client).execute("launch pad", 7)

        assertEquals("launch pad", result.query)
        assertTrue(result.projects.isEmpty())
        client.close()
    }

    @Test
    fun homeDeserializesPolymorphicSections() = runTest {
        val client = client(handler = {
            respond(
                """{"pageTitle":"Home","sections":[{"type":"creator_row","id":"artists","title":"Artists","items":[{"id":"artist","title":"Artist","username":"artist"}]}]}""",
                headers = jsonHeaders,
            )
        })

        val home = GetHomeUseCase(client).execute()

        val section = assertIs<HubHomeSection.CreatorRow>(home.sections.single())
        assertEquals("artist", section.items.single().username)
        assertNull(section.actionHref)
        client.close()
    }

    @Test
    fun apiErrorsExposeBackendCodeAndStatus() = runTest {
        val client = client(handler = {
            respond(
                """{"error":"rate_limited"}""",
                HttpStatusCode.TooManyRequests,
                headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                    HttpHeaders.RetryAfter to listOf("60"),
                ),
            )
        })

        val error = try {
            GetHealthUseCase(client).execute()
            null
        } catch (caught: HubApiException) {
            caught
        }

        assertEquals("rate_limited", error?.errorCode)
        assertEquals(429, error?.statusCode)
        assertEquals(60, error?.retryAfterSeconds)
        assertFalse(client.isAuthenticated)
        client.close()
    }

    private fun client(
        bearerToken: String? = null,
        refreshToken: String? = null,
        onSessionChanged: ((HubSessionTokens?) -> Unit)? = null,
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): HubApiClient {
        val http = HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false })
            }
        }
        return HubApiClient("https://hub.test", bearerToken, refreshToken, onSessionChanged, http)
    }

    private fun accountJson() =
        """{"id":"id","username":"artist","displayName":"Artist","bio":"","avatarUrl":"","email":null,"totpEnabled":false,"created":1}"""

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
}
