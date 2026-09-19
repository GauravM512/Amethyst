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
    val followersCount: Long,
    val isFollowing: Boolean,
)

@Serializable
data class HubProject(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val compatibility: HubProjectCompatibility,
    val projectType: HubProjectType? = null,
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
data class HubProjectInput(
    val title: String,
    val description: String,
    val compatibility: HubProjectCompatibility,
    val difficulty: Int,
    val youtubeUrl: String? = null,
)
