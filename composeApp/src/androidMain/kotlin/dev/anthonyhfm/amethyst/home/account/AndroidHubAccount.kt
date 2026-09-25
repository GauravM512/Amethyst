package dev.anthonyhfm.amethyst.home.account

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.anthonyhfm.amethyst.hub.data.HubAccount
import dev.anthonyhfm.amethyst.hub.data.HubAccountService
import dev.anthonyhfm.amethyst.hub.data.HubArtistProfileInput
import dev.anthonyhfm.amethyst.hub.data.HubAuthResult
import dev.anthonyhfm.amethyst.hub.data.HubAvatarInput
import dev.anthonyhfm.amethyst.hub.data.HubRepository
import dev.anthonyhfm.amethyst.hub.data.HubSessionStore
import dev.anthonyhfm.amethyst.hub.data.HubSessionTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

/** One session and repository for Profile and the future Android Hub screens. */
class AndroidHubAccount private constructor(context: Context) {
    val repository = HubRepository(sessionStore = AndroidHubTokenStore(context.applicationContext))
    private val service = HubAccountService(repository)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var account by mutableStateOf<HubAccount?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var mfaChallenge by mutableStateOf<String?>(null)
        private set
    var sessionRevision by mutableIntStateOf(0)
        private set

    init {
        if (repository.client.isAuthenticated) refresh()
    }

    fun clearMessages() {
        error = null
        message = null
    }

    fun showError(value: String) {
        error = value
    }

    fun prepareAuth() {
        clearMessages()
        mfaChallenge = null
    }

    fun refresh() {
        if (!repository.client.isAuthenticated) return
        runAction {
            acceptAccount(repository.getAccount.execute())
        }
    }

    fun signIn(username: String, password: String, register: Boolean, displayName: String, email: String) {
        runAction {
            val result = if (register) service.registerAndLogin(username, password, displayName, email)
            else service.login(username, password)
            handleAuth(result)
        }
    }

    fun completeMfa(code: String) {
        val challenge = mfaChallenge ?: return
        runAction { handleAuth(service.completeMfa(challenge, code)) }
    }

    fun requestReset(username: String, success: String) {
        runAction {
            repository.requestPasswordReset.execute(username.trim().replace("@", ""))
            message = success
        }
    }

    fun updateProfile(displayName: String, bio: String, onSuccess: () -> Unit) {
        runAction {
            acceptAccount(repository.updateArtistProfile.execute(HubArtistProfileInput(displayName, bio)))
            onSuccess()
        }
    }

    fun updateAvatar(bytes: ByteArray, mimeType: String) {
        runAction {
            val encoded = withContext(Dispatchers.Default) { Base64.encodeToString(bytes, Base64.NO_WRAP) }
            acceptAccount(repository.setAccountAvatar.execute(HubAvatarInput(encoded, mimeType)))
        }
    }

    fun removeAvatar() {
        runAction { acceptAccount(repository.removeAccountAvatar.execute()) }
    }

    fun changePassword(current: String, replacement: String, onSuccess: () -> Unit) {
        val username = account?.username ?: return
        runAction {
            service.changePassword(username, current, replacement)
            // The server revokes the session after a password change.
            clearAccount()
            onSuccess()
        }
    }

    fun changeEmail(password: String, email: String, code: String, success: String) {
        val username = account?.username ?: return
        runAction {
            service.changeEmail(username, password, email, code)
            message = success
        }
    }

    fun removeEmail(password: String, code: String, success: String) {
        val username = account?.username ?: return
        runAction {
            service.removeEmail(username, password, code)
            acceptAccount(repository.getAccount.execute())
            message = success
        }
    }

    fun signOut() {
        runAction {
            try { service.signOut() }
            finally { clearAccount() }
        }
    }

    private suspend fun handleAuth(result: HubAuthResult) {
        mfaChallenge = result.challenge
        if (result.challenge != null) return
        val resolved = result.account ?: repository.getAccount.execute()
        acceptAccount(resolved)
    }

    private fun acceptAccount(value: HubAccount) {
        if (account == null) sessionRevision++
        account = value
        mfaChallenge = null
        clearMessages()
    }

    private fun clearAccount() {
        val hadSession = account != null || repository.client.isAuthenticated
        repository.client.clearSession()
        account = null
        mfaChallenge = null
        if (hadSession) sessionRevision++
    }

    private fun runAction(block: suspend () -> Unit) {
        if (busy) return
        clearMessages()
        busy = true
        scope.launch {
            try { block() }
            catch (cause: Exception) { error = cause.message ?: cause.toString() }
            finally { busy = false }
        }
    }

    companion object {
        @Volatile private var instance: AndroidHubAccount? = null

        fun get(context: Context): AndroidHubAccount = instance ?: synchronized(this) {
            instance ?: AndroidHubAccount(context).also { instance = it }
        }
    }
}

/** Android Keystore encrypts both tokens before the app persists them. */
private class AndroidHubTokenStore(context: Context) : HubSessionStore {
    private val preferences = context.getSharedPreferences("hub_session", Context.MODE_PRIVATE)
    private val alias = "amethyst.hub.session"

    override fun load(): HubSessionTokens? = runCatching {
        val payload = preferences.getString("tokens", null) ?: return null
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        val ivSize = bytes[0].toInt() and 0xff
        require(ivSize in 12..16 && bytes.size > ivSize + 1)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 1, ivSize))
        val fields = cipher.doFinal(bytes, 1 + ivSize, bytes.size - ivSize - 1)
            .decodeToString().split('\n', limit = 2)
        HubSessionTokens(fields[0], fields[1], 300)
    }.getOrNull()

    override fun save(tokens: HubSessionTokens?) {
        if (tokens == null) {
            preferences.edit().remove("tokens").apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal("${tokens.accessToken}\n${tokens.refreshToken}".encodeToByteArray())
        val bytes = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + encrypted
        preferences.edit().putString("tokens", Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }
}
