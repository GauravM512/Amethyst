package dev.anthonyhfm.amethyst.hub.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HubHome(
    val pageTitle: String,
    val sections: List<HubHomeSection>,
)

@Serializable
sealed interface HubHomeSection {
    val id: String
    val title: String
    val actionLabel: String?
    val actionHref: String?

    @Serializable
    @SerialName("creator_row")
    data class CreatorRow(
        override val id: String,
        override val title: String,
        override val actionLabel: String? = null,
        override val actionHref: String? = null,
        val items: List<HubCreatorItem>,
    ) : HubHomeSection

    @Serializable
    @SerialName("hero_carousel")
    data class HeroCarousel(
        override val id: String,
        override val title: String,
        override val actionLabel: String? = null,
        override val actionHref: String? = null,
        val items: List<HubHeroProjectItem>,
    ) : HubHomeSection

    @Serializable
    @SerialName("square_card_row")
    data class SquareCardRow(
        override val id: String,
        override val title: String,
        override val actionLabel: String? = null,
        override val actionHref: String? = null,
        val items: List<HubSquareCardItem>,
    ) : HubHomeSection

    @Serializable
    @SerialName("media_card_row")
    data class MediaCardRow(
        override val id: String,
        override val title: String,
        override val actionLabel: String? = null,
        override val actionHref: String? = null,
        val items: List<HubMediaCardItem>,
    ) : HubHomeSection

    @Serializable
    @SerialName("detailed_list")
    data class DetailedList(
        override val id: String,
        override val title: String,
        override val actionLabel: String? = null,
        override val actionHref: String? = null,
        val items: List<HubDetailedListItem>,
    ) : HubHomeSection

    @Serializable
    @SerialName("curated_spotlight")
    data class CuratedSpotlight(
        override val id: String,
        override val title: String,
        override val actionLabel: String? = null,
        override val actionHref: String? = null,
        val creatorName: String,
        val creatorAvatarUrl: String? = null,
        val description: String,
        val items: List<HubSquareCardItem>,
    ) : HubHomeSection
}

@Serializable
data class HubCreatorItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val badge: String? = null,
    val href: String? = null,
    val username: String,
    val isFollowing: Boolean? = null,
    val followersCount: Long? = null,
    val verified: Boolean? = null,
)

@Serializable
data class HubHeroProjectItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val badge: String? = null,
    val href: String? = null,
    val creatorName: String,
    val creatorAvatarUrl: String? = null,
    val hardwareModel: String? = null,
    val bpm: Int? = null,
    val key: String? = null,
    val duration: String? = null,
    val previewAudioUrl: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class HubSquareCardItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val badge: String? = null,
    val href: String? = null,
    val itemCount: Long? = null,
    val iconName: String? = null,
    val colorAccent: String? = null,
)

@Serializable
data class HubMediaCardItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val badge: String? = null,
    val href: String? = null,
    val artist: String,
    val downloadsCount: Long? = null,
    val likesCount: Long? = null,
    val hardware: String? = null,
)

@Serializable
data class HubDetailedListItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val badge: String? = null,
    val href: String? = null,
    val description: String,
    val uploadedAt: String,
    val compatibility: String,
    val fileSize: String? = null,
    val hasLightshow: Boolean? = null,
    val tags: List<String> = emptyList(),
)
