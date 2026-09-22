package dev.anthonyhfm.amethyst.hub.data

import kotlinx.serialization.Serializable

@Serializable
enum class HubProjectCompatibility {
    compatible,
    partially,
    incompatible,
    unknown
}

@Serializable
enum class HubProjectType {
    amethyst,
    ableton,
    apollo,
    unipad
}

@Serializable
enum class HubProjectStatus {
    draft,
    public
}

@Serializable
enum class HubProjectSort {
    popular,
    views,
    newest,
    title,
    difficulty
}

@Serializable
data class HubArtistSummary(
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
)

@Serializable
data class HubArtist(
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String,
    val created: Long,
    val publishedProjectCount: Long,
    val followersCount: Long = 0,
    val isFollowing: Boolean = false,
)

@Serializable
data class HubProject(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val compatibility: HubProjectCompatibility,
    val projectType: HubProjectType = HubProjectType.amethyst,
    val views: Long = 0,
    val downloadsCount: Long = 0,
    val likesCount: Long = 0,
    val isLiked: Boolean = false,
    val youtubeUrl: String? = null,
    val difficulty: Int = 0,
    val status: HubProjectStatus,
    val artist: HubArtistSummary,
    val created: Long,
    val updated: Long,
    val publishedAt: Long? = null,
    val packageName: String? = null,
    val packageSize: Long? = null,
    val packageSha256: String? = null,
    val downloadUrl: String? = null,
    val thumbnailUrl: String? = null,
    val overrideName: String? = null,
    val overrideSize: Long? = null,
    val overrideSha256: String? = null,
    val overrideDownloadUrl: String? = null,
)

@Serializable
data class HubProjectPage(
    val items: List<HubProject>,
    val nextCursor: String? = null,
)

@Serializable
data class HubFollowResult(
    val following: Boolean,
    val followersCount: Long,
)

@Serializable
data class HubLikeResult(
    val liked: Boolean,
    val likesCount: Long,
)

@Serializable
data class HubProjectViewResult(
    val ok: Boolean,
    val views: Long,
)

@Serializable
data class HubArtistPage(
    val items: List<HubArtist>,
    val nextCursor: String? = null,
)

@Serializable
data class HubProjectInput(
    val title: String,
    val description: String,
    val compatibility: HubProjectCompatibility,
    val difficulty: Int,
    val youtubeUrl: String? = null,
    val projectType: HubProjectType = HubProjectType.amethyst,
)

@Serializable
data class HubSearchResult(
    val query: String,
    val projects: List<HubProject>,
    val artists: List<HubArtist>,
    val projectCount: Long,
    val artistCount: Long,
)

@Serializable
data class HubOk(val ok: Boolean)

@Serializable
data class HubHealth(val ok: Boolean)
