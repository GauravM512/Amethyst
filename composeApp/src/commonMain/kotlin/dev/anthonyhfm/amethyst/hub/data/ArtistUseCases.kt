package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.request.*
import io.ktor.http.encodeURLPathPart

class GetArtistUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(username: String): HubArtist {
        return client.optionallyAuthorized { token ->
            client.http.get("${client.baseUrl}/artists/${username.encodeURLPathPart()}") {
                applyBearerAuth(token)
            }
        }.hubBody()
    }
}

class GetArtistProjectsUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(username: String, cursor: String? = null, limit: Int = 24): HubProjectPage {
        return client.optionallyAuthorized { token ->
            client.http.get("${client.baseUrl}/artists/${username.encodeURLPathPart()}/projects") {
                applyBearerAuth(token)
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit)
            }
        }.hubBody()
    }
}

class GetPublishedProjectUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(username: String, slug: String): HubProject {
        return client.optionallyAuthorized { token ->
            client.http.get("${client.baseUrl}/artists/${username.encodeURLPathPart()}/projects/${slug.encodeURLPathPart()}") {
                applyBearerAuth(token)
            }
        }.hubBody()
    }
}

class GetPublishedProjectByIdUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(projectId: String): HubProject {
        return client.optionallyAuthorized { token ->
            client.http.get(urlString = "${client.baseUrl}/projects/${projectId.encodeURLPathPart()}") {
                applyBearerAuth(token = token)
            }
        }.hubBody()
    }
}

class BrowseArtistsUseCase(private val client: HubApiClient) {
    suspend fun execute(cursor: String? = null, limit: Int = 24): HubArtistPage {
        return client.optionallyAuthorized { token ->
            client.http.get("${client.baseUrl}/artists") {
                applyBearerAuth(token)
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit)
            }
        }.hubBody()
    }
}

class DownloadArtistAvatarUseCase(private val client: HubApiClient) {
    suspend fun execute(username: String): ByteArray =
        client.http.get("${client.baseUrl}/avatars/${username.encodeURLPathPart()}").hubBody()
}

class GetArtistCollectionsUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(username: String): List<HubProjectCollection> {
        return client.optionallyAuthorized { token ->
            client.http.get("${client.baseUrl}/artists/${username.encodeURLPathPart()}/collections") {
                applyBearerAuth(token)
            }
        }.hubBody()
    }
}
