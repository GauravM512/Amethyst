package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.request.*
import io.ktor.http.*

class GetOwnedProjectsUseCase(private val client: HubApiClient) {
    suspend fun execute(cursor: String? = null, limit: Int = 24): HubProjectPage {
        return client.authorized { token ->
            client.http.get("${client.baseUrl}/v1/account/projects") {
                applyBearerAuth(token)
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit)
            }
        }.hubBody()
    }
}

class GetOwnedProjectUseCase(private val client: HubApiClient) {

    suspend fun execute(projectId: String): HubProject {
        return client.authorized { token ->
            client.http.get("${client.baseUrl}/v1/account/projects/${projectId.encodeURLPathPart()}") {
                applyBearerAuth(token)
            }
        }.hubBody()
    }
}

class CreateProjectUseCase(private val client: HubApiClient) {
    suspend fun execute(input: HubProjectInput): HubProject {
        return client.authorized { token ->
            client.http.post("${client.baseUrl}/v1/account/projects") {
                applyBearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(input)
            }
        }.hubBody()
    }
}

class UpdateProjectUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String, input: HubProjectInput): HubProject {
        return client.authorized { token ->
            client.http.patch("${client.baseUrl}/v1/account/projects/${projectId.encodeURLPathPart()}") {
                applyBearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(input)
            }
        }.hubBody()
    }
}

class PublishProjectUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String): HubProject {
        return client.authorized { token ->
            client.http.post("${client.baseUrl}/v1/account/projects/${projectId.encodeURLPathPart()}/publish") {
                applyBearerAuth(token)
            }
        }.hubBody()
    }
}

class UnpublishProjectUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String): HubProject {
        return client.authorized { token ->
            client.http.post("${client.baseUrl}/v1/account/projects/${projectId.encodeURLPathPart()}/unpublish") {
                applyBearerAuth(token)
            }
        }.hubBody()
    }
}

class DeleteProjectUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String): HubOk {
        return client.authorized { token ->
            client.http.delete("${client.baseUrl}/v1/account/projects/${projectId.encodeURLPathPart()}") {
                applyBearerAuth(token)
            }
        }.hubBody()
    }
}

class UploadProjectPackageUseCase(private val client: HubApiClient) {

    suspend fun execute(
        projectId: String,
        fileName: String,
        bytes: ByteArray,
    ): HubProject {
        return client.authorized { token ->
            client.http.post("${client.baseUrl}/v1/account/projects/${projectId.encodeURLPathPart()}/package") {
                applyBearerAuth(token)
                header("X-File-Name", fileName)
                contentType(ContentType.Application.OctetStream)
                setBody(bytes)
            }
        }.hubBody()
    }
}

class UploadProjectThumbnailUseCase(private val client: HubApiClient) {

    suspend fun execute(
        projectId: String,
        bytes: ByteArray,
        mimeType: ContentType = ContentType.Image.PNG,
    ): HubProject {
        return client.authorized { token ->
            client.http.post("${client.baseUrl}/v1/account/projects/${projectId.encodeURLPathPart()}/thumbnail") {
                applyBearerAuth(token)
                contentType(mimeType)
                setBody(bytes)
            }
        }.hubBody()
    }
}

class DeleteProjectThumbnailUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String): HubProject {
        return client.authorized { token ->
            client.http.delete("${client.baseUrl}/v1/account/projects/${projectId.encodeURLPathPart()}/thumbnail") {
                applyBearerAuth(token)
            }
        }.hubBody()
    }
}

class GetLikedProjectsUseCase(private val client: HubApiClient) {
    suspend fun execute(cursor: String? = null, limit: Int = 24): HubProjectPage {
        return client.authorized { token ->
            client.http.get("${client.baseUrl}/v1/account/projects/liked") {
                applyBearerAuth(token)
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit)
            }
        }.hubBody()
    }
}

class ToggleProjectLikeUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(projectId: String): HubLikeResult {
        return client.authorized { token ->
            client.http.post("${client.baseUrl}/v1/account/projects/${projectId.encodeURLPathPart()}/like") {
                applyBearerAuth(token)
            }
        }.hubBody()
    }
}

class UploadProjectOverrideUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String, fileName: String, bytes: ByteArray): HubProject {
        return client.authorized { token ->
            client.http.post("${client.baseUrl}/v1/account/projects/${projectId.encodeURLPathPart()}/override") {
                applyBearerAuth(token)
                header("X-File-Name", fileName)
                contentType(ContentType.Application.OctetStream)
                setBody(bytes)
            }
        }.hubBody()
    }
}

class DeleteProjectOverrideUseCase(private val client: HubApiClient) {
    suspend fun execute(projectId: String): HubProject {
        return client.authorized { token ->
            client.http.delete("${client.baseUrl}/v1/account/projects/${projectId.encodeURLPathPart()}/override") {
                applyBearerAuth(token)
            }
        }.hubBody()
    }
}
