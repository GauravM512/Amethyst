package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.setBody
import io.ktor.http.*

class GetOwnedProjectsUseCase(private val client: HubApiClient) {
    suspend fun execute(): HubProjectPage {
        return client.http.get("${client.baseUrl}/v1/account/projects") {
            applyBearerAuth(client.bearerToken)
        }.body()
    }
}

class GetOwnedProjectUseCase(private val client: HubApiClient) {

    suspend fun execute(projectId: String): HubProject {
        return client.http.get("${client.baseUrl}/v1/account/projects/$projectId") {
            applyBearerAuth(client.bearerToken)
        }.body()
    }
}

class CreateProjectUseCase(private val client: HubApiClient) {
    suspend fun execute(input: HubProjectInput): HubProject {
        return client.http.post("${client.baseUrl}/v1/account/projects") {
            applyBearerAuth(client.bearerToken)
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }
}

class UpdateProjectUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String, input: HubProjectInput) {
        client.http.patch("${client.baseUrl}/v1/account/projects/$projectId") {
            applyBearerAuth(client.bearerToken)
            contentType(ContentType.Application.Json)
            setBody(input)
        }
    }
}

class PublishProjectUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String) {
        client.http.post("${client.baseUrl}/v1/account/projects/$projectId/publish") {
            applyBearerAuth(client.bearerToken)
        }
    }
}

class UnpublishProjectUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String) {
        client.http.post("${client.baseUrl}/v1/account/projects/$projectId/unpublish") {
            applyBearerAuth(client.bearerToken)
        }
    }
}

class DeleteProjectUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String) {
        client.http.delete("${client.baseUrl}/v1/account/projects/$projectId") {
            applyBearerAuth(client.bearerToken)
        }
    }
}

class UploadProjectPackageUseCase(private val client: HubApiClient) {

    suspend fun execute(
        projectId: String,
        fileName: String,
        bytes: ByteArray,
    ): HubProject {
        return client.http.post("${client.baseUrl}/v1/account/projects/$projectId/package") {
            applyBearerAuth(client.bearerToken)
            header("X-File-Name", fileName)
            contentType(ContentType.Application.OctetStream)
            setBody(bytes)
        }.body()
    }
}

class UploadProjectThumbnailUseCase(private val client: HubApiClient) {

    suspend fun execute(
        projectId: String,
        bytes: ByteArray,
        mimeType: ContentType = ContentType.Image.PNG,
    ): HubProject {
        return client.http.post("${client.baseUrl}/v1/account/projects/$projectId/thumbnail") {
            applyBearerAuth(client.bearerToken)
            contentType(mimeType)
            setBody(bytes)
        }.body()
    }
}

class DeleteProjectThumbnailUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String): HubProject {
        return client.http.delete("${client.baseUrl}/v1/account/projects/$projectId/thumbnail") {
            applyBearerAuth(client.bearerToken)
        }.body()
    }
}
