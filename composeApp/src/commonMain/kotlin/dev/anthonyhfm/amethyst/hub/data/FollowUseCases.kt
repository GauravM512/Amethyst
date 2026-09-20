package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.request.*
import io.ktor.http.encodeURLPathPart

class FollowArtistUseCase(private val client: HubApiClient) {
    suspend fun execute(username: String): HubFollowResult {
        return client.authorized { token ->
            client.http.put("${client.baseUrl}/v1/account/follows/${username.encodeURLPathPart()}") {
                applyBearerAuth(token)
            }
        }.hubBody()
    }
}

class UnfollowArtistUseCase(private val client: HubApiClient) {
    suspend fun execute(username: String): HubFollowResult {
        return client.authorized { token ->
            client.http.delete("${client.baseUrl}/v1/account/follows/${username.encodeURLPathPart()}") {
                applyBearerAuth(token)
            }
        }.hubBody()
    }
}
