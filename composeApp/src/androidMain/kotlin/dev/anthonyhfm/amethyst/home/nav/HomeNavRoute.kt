package dev.anthonyhfm.amethyst.home.nav

import kotlinx.serialization.Serializable

@Serializable
sealed interface HomeNavRoute {
    @Serializable
    data object Projects : HomeNavRoute

    @Serializable
    data object Browser : HomeNavRoute

    @Serializable
    data object Arcade : HomeNavRoute

    @Serializable
    data object Settings : HomeNavRoute

    @Serializable
    data object ProfileAuth : HomeNavRoute

    @Serializable
    data object ProfileEdit : HomeNavRoute

    @Serializable
    data object HubLiked : HomeNavRoute

    @Serializable
    data class HubDetail(val username: String, val slug: String? = null) : HomeNavRoute

    @Serializable
    data object ProjectCreation : HomeNavRoute

    @Serializable
    data class ProjectEdit(val projectPath: String) : HomeNavRoute

    @Serializable
    data class AbletonImportWizard(val liveSetPath: String) : HomeNavRoute

    @Serializable
    data class LoadingScreen(val text: String) : HomeNavRoute
}
