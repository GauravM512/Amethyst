package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HubDeepLinksTest {
    @Test
    fun parsesLegacyAndEncodedWebLinks() {
        assertEquals(
            expected = HubProjectDeepLink(projectId = "project_1"),
            actual = HubDeepLinks.parse(value = "AMETHYST://PROJECT/project_1"),
        )
        assertEquals(
            expected = HubProjectDeepLink(projectId = "project_1", username = "artist", slug = "song-one"),
            actual = HubDeepLinks.parse(value = "amethyst://project/project%5F1?username=artist&slug=song%2Done"),
        )
        assertEquals(
            expected = HubProjectDeepLink(projectId = "project_1", username = "artist+name", slug = "a song & lightshow"),
            actual = HubDeepLinks.parse(value = "amethyst://project/project_1?username=artist%2Bname&slug=a+song+%26+lightshow"),
        )
    }

    @Test
    fun rejectsOtherHostsSchemesAndInvalidIds() {
        listOf(
            "https://project/project_1",
            "amethyst://artist/project_1",
            "amethyst://project/",
            "amethyst://project/project_1/other",
            "amethyst://project/%2Fsecret",
            "amethyst://project/%00",
            "amethyst://project/project+one",
            "amethyst://project@evil/project_1",
        ).forEach { value ->
            assertNull(actual = HubDeepLinks.parse(value = value), message = value)
        }
    }

    @Test
    fun resolvesWebHintsUsingExistingArtistEndpoint() = runTest {
        val repository = repository { request ->
            assertEquals(expected = "/artists/artist/projects/song-one", actual = request.url.encodedPath)
            respond(content = projectJson(id = "project_1"), headers = jsonHeaders)
        }
        try {
            val link = assertNotNull(actual = HubDeepLinks.parse(value = "amethyst://project/project_1?username=artist&slug=song-one"))
            assertEquals(expected = "project_1", actual = link.resolve(repository = repository).id)
        } finally {
            repository.close()
        }
    }

    @Test
    fun legacyIdLinksUsePublicLookupWithBearerToken() = runTest {
        val repository = repository(bearerToken = "access") { request ->
            assertEquals(expected = "/projects/project_1", actual = request.url.encodedPath)
            assertEquals(expected = "Bearer access", actual = request.headers[HttpHeaders.Authorization])
            respond(content = projectJson(id = "project_1"), headers = jsonHeaders)
        }
        try {
            assertEquals(expected = "project_1", actual = HubProjectDeepLink(projectId = "project_1").resolve(repository = repository).id)
        } finally {
            repository.close()
        }
    }

    @Test
    fun wrongArtistHintsCannotOpenADifferentProject() = runTest {
        val requests = mutableListOf<String>()
        val repository = repository { request ->
            requests.add(element = request.url.encodedPath)
            respond(
                content = projectJson(id = if (request.url.encodedPath.startsWith(prefix = "/artists/")) "other" else "project_1"),
                headers = jsonHeaders,
            )
        }
        try {
            val project = HubProjectDeepLink(projectId = "project_1", username = "artist", slug = "wrong").resolve(repository = repository)
            assertEquals(expected = "project_1", actual = project.id)
            assertEquals(expected = listOf("/artists/artist/projects/wrong", "/projects/project_1"), actual = requests)
        } finally {
            repository.close()
        }
    }

    @Test
    fun legacyServersFallBackThroughEveryBrowsePage() = runTest {
        val cursors = mutableListOf<String?>()
        val repository = repository { request ->
            if (request.url.encodedPath == "/projects/project_1") {
                respond(content = """{"error":"not_found"}""", status = HttpStatusCode.NotFound, headers = jsonHeaders)
            } else {
                val cursor = request.url.parameters["cursor"]
                cursors.add(element = cursor)
                assertEquals(expected = "50", actual = request.url.parameters["limit"])
                respond(
                    content = if (cursor == null) """{"items":[${projectJson(id = "other")}],"nextCursor":"next"}"""
                    else """{"items":[${projectJson(id = "project_1")}]}""",
                    headers = jsonHeaders,
                )
            }
        }
        try {
            assertEquals(expected = "project_1", actual = HubProjectDeepLink(projectId = "project_1").resolve(repository = repository).id)
            assertEquals(expected = listOf(null, "next"), actual = cursors)
        } finally {
            repository.close()
        }
    }

    @Test
    fun missingProjectsStopAtEndOfCatalogue() = runTest {
        val repository = repository { request ->
            if (request.url.encodedPath == "/projects/missing") {
                respond(content = """{"error":"not_found"}""", status = HttpStatusCode.NotFound, headers = jsonHeaders)
            } else {
                respond(content = """{"items":[]}""", headers = jsonHeaders)
            }
        }
        try {
            var error: HubApiException? = null
            try {
                HubProjectDeepLink(projectId = "missing").resolve(repository = repository)
            } catch (caught: HubApiException) {
                error = caught
            }
            assertEquals(expected = 404, actual = error?.statusCode)
        } finally {
            repository.close()
        }
    }

    @Test
    fun serverErrorsDoNotFallBackToCatalogueRequests() = runTest {
        var requests = 0
        val repository = repository {
            requests += 1
            respond(content = """{"error":"rate_limited"}""", status = HttpStatusCode.TooManyRequests, headers = jsonHeaders)
        }
        try {
            var error: HubApiException? = null
            try {
                HubProjectDeepLink(projectId = "project_1").resolve(repository = repository)
            } catch (caught: HubApiException) {
                error = caught
            }
            assertEquals(expected = 429, actual = error?.statusCode)
            assertEquals(expected = 1, actual = requests)
        } finally {
            repository.close()
        }
    }

    @Test
    fun repeatedCatalogueCursorsCannotLoopForever() = runTest {
        var pages = 0
        val repository = repository { request ->
            if (request.url.encodedPath == "/projects/missing") {
                respond(content = """{"error":"not_found"}""", status = HttpStatusCode.NotFound, headers = jsonHeaders)
            } else {
                pages += 1
                respond(content = """{"items":[],"nextCursor":"repeated"}""", headers = jsonHeaders)
            }
        }
        try {
            var error: HubApiException? = null
            try {
                HubProjectDeepLink(projectId = "missing").resolve(repository = repository)
            } catch (caught: HubApiException) {
                error = caught
            }
            assertEquals(expected = 404, actual = error?.statusCode)
            assertEquals(expected = 2, actual = pages)
        } finally {
            repository.close()
        }
    }

    private fun repository(
        bearerToken: String? = null,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HubRepository {
        val http = HttpClient(engine = MockEngine(handler = handler)) {
            install(plugin = ContentNegotiation) {
                json(json = Json { ignoreUnknownKeys = true })
            }
        }
        return HubRepository(
            client = HubApiClient(
                baseUrl = "https://hub.test",
                bearerToken = bearerToken,
                refreshToken = null,
                onSessionChanged = null,
                http = http,
            )
        )
    }

    private fun projectJson(id: String) =
        """{"id":"$id","slug":"song-one","title":"Song","description":"","compatibility":"compatible","status":"public","artist":{"username":"artist","displayName":"Artist"},"created":1,"updated":1}"""

    private val jsonHeaders = headersOf(name = HttpHeaders.ContentType, value = ContentType.Application.Json.toString())
}
