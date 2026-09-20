package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.request.get

class GetHomeUseCase(private val client: HubApiClient) {
    suspend fun execute(): HubHome = client.optionallyAuthorized { token ->
        client.http.get("${client.baseUrl}/home") { applyBearerAuth(token) }
    }.hubBody()
}

class SearchHubUseCase(private val client: HubApiClient) {
    suspend fun execute(query: String, limit: Int = 12): HubSearchResult =
        client.optionallyAuthorized { token ->
            client.http.get("${client.baseUrl}/search") {
                applyBearerAuth(token)
                url.parameters.append("q", query)
                url.parameters.append("limit", limit.toString())
            }
        }.hubBody()
}

class GetHealthUseCase(private val client: HubApiClient) {
    suspend fun execute(): HubHealth = client.http.get("${client.baseUrl}/health").hubBody()
}
