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
import kotlin.test.assertTrue

class HubAccountServiceTest {
    @Test
    fun repositoryRestoresAndClearsStoredSession() {
        val store = object : HubSessionStore {
            var tokens: HubSessionTokens? = HubSessionTokens("saved-access", "saved-refresh", 300)
            override fun load(): HubSessionTokens? = tokens
            override fun save(tokens: HubSessionTokens?) { this.tokens = tokens }
        }
        val repository = HubRepository("https://hub.test", store)

        assertEquals("saved-access", repository.client.bearerToken)
        repository.client.clearSession()
        assertEquals(null, store.tokens)
        repository.close()
    }

    @Test
    fun prehashMatchesTheExistingIosAndServerFormat() = runTest {
        assertEquals(
            "\$prehash\$v1\$TOyLAlrP1c-pmF_UQR5h6_T5T0oY1QAAJENq9J6ZEq8",
            PasswordPrehash.derive("password", " Kaskobi "),
        )
    }

    @Test
    fun loginRetriesOnlyInvalidCredentialsForLegacyAccounts() = runTest {
        var calls = 0
        val http = HttpClient(MockEngine {
            calls++
            if (calls == 1) {
                respond("""{"error":"invalid_credentials"}""", HttpStatusCode.Unauthorized, jsonHeaders)
            } else {
                respond("""{"accessToken":"access","refreshToken":"refresh","expiresIn":300}""", headers = jsonHeaders)
            }
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val repository = HubRepository(HubApiClient("https://hub.test", null, null, null, http))

        val result = HubAccountService(repository).login("artist", "legacy-password")

        assertEquals(2, calls)
        assertTrue(result.isAuthenticated)
        assertTrue(repository.client.isAuthenticated)
        repository.close()
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
}
