package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class GetAccountUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(): HubAccount = client.authorized { token ->
        client.http.get("${client.baseUrl}/v1/account") { applyBearerAuth(token) }
    }.hubBody()
}

class UpdateArtistProfileUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubArtistProfileInput): HubAccount = client.authorized { token ->
        client.http.patch("${client.baseUrl}/v1/account/artist") {
            applyBearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(input)
        }
    }.hubBody()
}

class SetAccountAvatarUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubAvatarInput): HubAccount = client.authorized { token ->
        client.http.post("${client.baseUrl}/v1/account/avatar") {
            applyBearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(input)
        }
    }.hubBody()
}

class RemoveAccountAvatarUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(): HubAccount = client.authorized { token ->
        client.http.post("${client.baseUrl}/v1/account/avatar-remove") {
            applyBearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(HubEmptyInput)
        }
    }.hubBody()
}

class GetSessionsUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(): HubSessions = client.authorized { token ->
        client.http.get("${client.baseUrl}/v1/account/sessions") { applyBearerAuth(token) }
    }.hubBody()
}

class ExportAccountUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(): HubAccountExport = client.authorized { token ->
        client.http.get("${client.baseUrl}/v1/account/export") { applyBearerAuth(token) }
    }.hubBody()
}

class LogoutUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(sessionId: String? = null, all: Boolean = false): HubOk {
        val result = client.authorized { token ->
            client.http.post("${client.baseUrl}/v1/account/logout") {
                applyBearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(HubLogoutInput(sessionId, all))
            }
        }.hubBody<HubOk>()
        if (all || sessionId == null) client.clearSession()
        return result
    }
}

class ChangePasswordUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubPasswordChangeInput): HubOk =
        client.revokingAccountRequest("password", input)
}

class ChangeEmailUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubEmailChangeInput): HubOk = client.authorized { token ->
        client.http.post("${client.baseUrl}/v1/account/email") {
            applyBearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(input)
        }
    }.hubBody()
}

class RemoveEmailUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubSensitiveInput): HubOk = client.authorized { token ->
        client.http.post("${client.baseUrl}/v1/account/email-remove") {
            applyBearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(input)
        }
    }.hubBody()
}

class SetupTotpUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubSensitiveInput): HubTotpSetup = client.authorized { token ->
        client.http.post("${client.baseUrl}/v1/account/totp-setup") {
            applyBearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(input)
        }
    }.hubBody()
}

class ConfirmTotpUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubSensitiveInput): HubRecoveryCodes =
        client.revokingAccountRequest("totp-confirm", input)
}

class DisableTotpUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubSensitiveInput): HubOk =
        client.revokingAccountRequest("totp-disable", input)
}

class RegenerateRecoveryCodesUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubSensitiveInput): HubRecoveryCodes = client.authorized { token ->
        client.http.post("${client.baseUrl}/v1/account/recovery-codes") {
            applyBearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(input)
        }
    }.hubBody()
}

class DeleteAccountUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubSensitiveInput): HubOk =
        client.revokingAccountRequest("delete", input)
}

private suspend inline fun <reified I, reified O> HubApiClient.revokingAccountRequest(
    action: String,
    input: I,
): O {
    val result = authorized { token ->
        http.post("$baseUrl/v1/account/$action") {
            applyBearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(input)
        }
    }.hubBody<O>()
    clearSession()
    return result
}

@kotlinx.serialization.Serializable
private data object HubEmptyInput
