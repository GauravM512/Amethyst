package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.call.*
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
        return client.http.get("${client.baseUrl}/projects") {
            cursor?.let { parameter("cursor", it) }
            parameter("limit", limit)
            compatibility?.let { parameter("compatibility", it.name) }
            type?.let { parameter("type", it.name) }
            sort?.let { parameter("sort", it.name) }
            query?.let { parameter("q", it) }
            difficulty?.let { parameter("difficulty", it) }
        }.body()
    }
}

class DownloadProjectPackageUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String): ByteArray {
        return client.http.get("${client.baseUrl}/projects/$projectId/download").body()
    }
}
