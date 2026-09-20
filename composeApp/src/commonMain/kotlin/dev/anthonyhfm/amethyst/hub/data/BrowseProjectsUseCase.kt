package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.request.*
import io.ktor.http.*

class BrowseProjectsUseCase(private val client: HubApiClient) {
    suspend fun execute(
        cursor: String? = null,
        limit: Int = 24,
        compatibility: HubProjectCompatibility? = null,
        type: HubProjectType? = null,
        sort: HubProjectSort? = null,
        query: String? = null,
        difficulty: String? = null,
    ): HubProjectPage {
        return client.optionallyAuthorized { token ->
            client.http.get("${client.baseUrl}/projects") {
                applyBearerAuth(token)
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit)
                compatibility?.let { parameter("compatibility", it.name) }
                type?.let { parameter("type", it.name) }
                sort?.let { parameter("sort", it.name) }
                query?.let { parameter("q", it) }
                difficulty?.let { parameter("difficulty", it) }
            }
        }.hubBody()
    }
}

class DownloadProjectPackageUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String): ByteArray {
        return client.http.get("${client.baseUrl}/projects/${projectId.encodeURLPathPart()}/download").hubBody()
    }
}

class DownloadProjectThumbnailUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String): ByteArray =
        client.http.get("${client.baseUrl}/projects/${projectId.encodeURLPathPart()}/thumbnail").hubBody()
}

class DownloadProjectOverrideUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String): ByteArray =
        client.http.get("${client.baseUrl}/projects/${projectId.encodeURLPathPart()}/override").hubBody()
}

class RecordProjectViewUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String): HubProjectViewResult =
        client.optionallyAuthorized { token ->
            client.http.post("${client.baseUrl}/projects/${projectId.encodeURLPathPart()}/view") {
                applyBearerAuth(token)
            }
        }.hubBody()
}
