package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.call.*
import io.ktor.client.request.*

class GetArtistUseCase(private val client: HubApiClient) {
    suspend fun execute(username: String): HubArtist {
        return client.http.get("${client.baseUrl}/artists/$username") {
            applyBearerAuth(client.bearerToken)
        }.body()
    }
}

class GetArtistProjectsUseCase(private val client: HubApiClient) {
    suspend fun execute(username: String): HubProjectPage {
        return client.http.get("${client.baseUrl}/artists/$username/projects").body()
    }
}

class GetPublishedProjectUseCase(private val client: HubApiClient) {
    suspend fun execute(username: String, slug: String): HubProject {
        return client.http.get("${client.baseUrl}/artists/$username/projects/$slug") {
            applyBearerAuth(client.bearerToken)
        }.body()
    }
}
