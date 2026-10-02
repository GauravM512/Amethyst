package dev.anthonyhfm.amethyst.hub.data

import kotlinx.serialization.Serializable

@Serializable
data class HubAuthConfig(val emailEnabled: Boolean = false)

@Serializable
data class HubAccount(
    val id: String,
    val username: String,
    val displayName: String,
    val bio: String,
    val avatarUrl: String? = null,
    val email: String? = null,
    val totpEnabled: Boolean,
    val created: Long,
)

@Serializable
data class HubAuthResult(
    val challenge: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val sessionToken: String? = null,
    val expiresIn: Long? = null,
    val account: HubAccount? = null,
) {
    val requiresMfa: Boolean get() = challenge != null
    val isAuthenticated: Boolean get() = accessToken != null && refreshToken != null
}

@Serializable
data class HubSessionTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

@Serializable
data class HubRegisterInput(
    val username: String,
    val password: String,
    val displayName: String = "",
    val email: String = "",
)

@Serializable
data class HubLoginInput(
    val username: String,
    val password: String,
    val remember: Boolean = true,
)

@Serializable
data class HubMfaInput(val challenge: String, val code: String)

@Serializable
internal data class HubRefreshInput(val refreshToken: String)

@Serializable
data class HubPasswordResetRequestInput(val username: String)

@Serializable
data class HubPasswordResetInput(
    val token: String,
    val newPassword: String,
    val code: String = "",
)

@Serializable
data class HubEmailConfirmationInput(val token: String)

@Serializable
data class HubSession(
    val id: String,
    val client: String,
    val created: Long,
    val lastSeen: Long,
    val current: Boolean,
)

@Serializable
data class HubSessions(val sessions: List<HubSession>)

@Serializable
data class HubSecurityEvent(val kind: String, val created: Long)

@Serializable
data class HubExportProject(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val compatibility: HubProjectCompatibility,
    val youtubeVideoId: String? = null,
    val difficulty: Int,
    val status: HubProjectStatus,
    val created: Long,
    val updated: Long,
    val publishedAt: Long? = null,
)

@Serializable
data class HubAccountExport(
    val account: HubAccount,
    val sessions: List<HubSession>,
    val projects: List<HubExportProject> = emptyList(),
    val securityEvents: List<HubSecurityEvent> = emptyList(),
)

@Serializable
data class HubArtistProfileInput(val displayName: String, val bio: String)

@Serializable
data class HubAvatarInput(
    val data: String = "",
    val mimeType: String = "image/png",
    val avatarUrl: String = "",
)

@Serializable
data class HubSensitiveInput(val password: String, val code: String = "")

@Serializable
data class HubPasswordChangeInput(
    val password: String,
    val newPassword: String,
    val code: String = "",
)

@Serializable
data class HubEmailChangeInput(
    val password: String,
    val email: String,
    val code: String = "",
)

@Serializable
data class HubLogoutInput(
    val sessionId: String? = null,
    val all: Boolean = false,
)

@Serializable
data class HubTotpSetup(val secret: String, val uri: String)

@Serializable
data class HubRecoveryCodes(val recoveryCodes: List<String>)

@Serializable
internal data class HubErrorResponse(val error: String)
