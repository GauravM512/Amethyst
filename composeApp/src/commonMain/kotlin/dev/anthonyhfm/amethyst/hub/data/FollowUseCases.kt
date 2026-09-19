package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.call.*
import io.ktor.client.request.*

class FollowArtistUseCase(private val client: HubApiClient) {
    suspend fun execute(username: String): HubFollowResult {
        return client.http.put("${client.baseUrl}/v1/account/follows/$username") {
            applyBearerAuth(client.bearerToken)
        }.body()
    }
}

class UnfollowArtistUseCase(private val client: HubApiClient) {
    suspend fun execute(username: String): HubFollowResult {
        return client.http.delete("${client.baseUrl}/v1/account/follows/$username") {
            applyBearerAuth(client.bearerToken)
        }.body()
    }
}
