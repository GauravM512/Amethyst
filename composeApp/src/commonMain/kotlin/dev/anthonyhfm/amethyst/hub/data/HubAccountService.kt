package dev.anthonyhfm.amethyst.hub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString

/** Authentication and sensitive account operations shared by every native client. */
class HubAccountService(private val repository: HubRepository) {
    @Throws(Exception::class)
    suspend fun login(username: String, password: String): HubAuthResult {
        val identifier = username.trim().replace("@", "")
        val prepared = PasswordPrehash.derive(password, identifier)
        return withLegacyCredential(password, prepared) { credential ->
            repository.login.execute(HubLoginInput(identifier, credential, remember = true))
        }
    }

    @Throws(Exception::class)
    suspend fun registerAndLogin(
        username: String,
        password: String,
        displayName: String,
        email: String,
    ): HubAuthResult {
        val identifier = username.trim().replace("@", "")
        repository.register.execute(
            input = HubRegisterInput(
                username = identifier,
                password = password,
                displayName = displayName,
                email = email,
            )
        )
        return repository.login.execute(
            input = HubLoginInput(
                username = identifier,
                password = password,
                remember = true,
            )
        )
    }

    @Throws(Exception::class)
    suspend fun completeMfa(challenge: String, code: String): HubAuthResult =
        repository.completeMfa.execute(HubMfaInput(challenge, code))

    @Throws(Exception::class)
    suspend fun changePassword(username: String, current: String, replacement: String): HubOk {
        val preparedCurrent = PasswordPrehash.derive(current, username)
        return withLegacyCredential(current, preparedCurrent) { credential ->
            repository.changePassword.execute(
                input = HubPasswordChangeInput(
                    password = credential,
                    newPassword = replacement,
                    code = "",
                )
            )
        }
    }

    @Throws(Exception::class)
    suspend fun changeEmail(username: String, password: String, email: String, code: String): HubOk {
        val prepared = PasswordPrehash.derive(password, username)
        return withLegacyCredential(password, prepared) { credential ->
            repository.changeEmail.execute(HubEmailChangeInput(credential, email, code))
        }
    }

    @Throws(Exception::class)
    suspend fun removeEmail(username: String, password: String, code: String): HubOk {
        val prepared = PasswordPrehash.derive(password, username)
        return withLegacyCredential(password, prepared) { credential ->
            repository.removeEmail.execute(HubSensitiveInput(credential, code))
        }
    }

    @Throws(Exception::class)
    suspend fun signOut() {
        try {
            repository.logout.execute(sessionId = null, all = false)
        } finally {
            if (repository.client.isAuthenticated) repository.client.clearSession()
        }
    }

    private suspend fun <T> withLegacyCredential(
        original: String,
        prepared: String,
        request: suspend (String) -> T,
    ): T = try {
        request(prepared)
    } catch (error: HubApiException) {
        if (error.errorCode != "invalid_credentials") throw error
        request(original)
    }
}

/** Server-compatible PBKDF2-HMAC-SHA256 prehash, independent of platform UI and crypto APIs. */
internal object PasswordPrehash {
    private const val ITERATIONS = 120_000
    private const val KEY_SIZE = 32

    suspend fun derive(password: String, username: String): String = withContext(Dispatchers.Default) {
        val key = password.encodeToByteArray().toByteString()
        val salt = "${username.trim().lowercase()}:amethyst-prehash-v1".encodeToByteArray()
        val block = salt + byteArrayOf(0, 0, 0, 1)
        var previous = block.toByteString().hmacSha256(key).toByteArray()
        val result = previous.copyOf()

        repeat(ITERATIONS - 1) {
            previous = previous.toByteString().hmacSha256(key).toByteArray()
            for (index in 0 until KEY_SIZE) {
                result[index] = (result[index].toInt() xor previous[index].toInt()).toByte()
            }
        }

        "\$prehash\$v1\$" + result.toByteString().base64Url().trimEnd('=')
    }
}
