package dev.anthonyhfm.amethyst.hub.data

import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class GetAuthConfigUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(): HubAuthConfig =
        client.http.get("${client.baseUrl}/v1/auth/config").hubBody()
}

class RegisterUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubRegisterInput): HubOk =
        client.http.post("${client.baseUrl}/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.hubBody()
}

class LoginUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubLoginInput): HubAuthResult {
        val result = client.http.post("${client.baseUrl}/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.hubBody<HubAuthResult>()
        client.acceptAuthentication(result)
        return result
    }
}

class CompleteMfaUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubMfaInput): HubAuthResult {
        val result = client.http.post("${client.baseUrl}/v1/auth/mfa") {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.hubBody<HubAuthResult>()
        client.acceptAuthentication(result)
        return result
    }
}

class RefreshSessionUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(): HubAuthResult = client.refreshSession()
}

class RequestPasswordResetUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(username: String): HubOk =
        client.http.post("${client.baseUrl}/v1/auth/reset-request") {
            contentType(ContentType.Application.Json)
            setBody(HubPasswordResetRequestInput(username))
        }.hubBody()
}

class ResetPasswordUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(input: HubPasswordResetInput): HubOk =
        client.http.post("${client.baseUrl}/v1/auth/reset") {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.hubBody()
}

class ConfirmEmailUseCase(private val client: HubApiClient) {
    @Throws(Exception::class)
    suspend fun execute(token: String): HubOk =
        client.http.post("${client.baseUrl}/v1/auth/email-confirm") {
            contentType(ContentType.Application.Json)
            setBody(HubEmailConfirmationInput(token))
        }.hubBody()
}
