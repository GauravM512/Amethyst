package dev.anthonyhfm.amethyst.home.nav

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Gamepad2
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.UserRound

import org.jetbrains.compose.resources.StringResource
import androidx.compose.runtime.Composable

enum class HomeNavigationTab(
    val labelRes: StringResource,
    val icon: ImageVector,
    val route: HomeNavRoute,
) {
    Projects(
        labelRes = Res.string.home_nav_tab_projects,
        icon = Lucide.History,
        route = HomeNavRoute.Projects,
    ),
    Browser(
        labelRes = Res.string.home_nav_tab_browser,
        icon = Lucide.FolderOpen,
        route = HomeNavRoute.Browser,
    ),
    Arcade(
        labelRes = Res.string.home_nav_tab_arcade,
        icon = Lucide.Gamepad2,
        route = HomeNavRoute.Arcade,
    ),
    Settings(
        labelRes = Res.string.profile_title,
        icon = Lucide.UserRound,
        route = HomeNavRoute.Settings,
    );

    val label: String @Composable get() = stringResource(labelRes)

    val routeName: String?
        get() = route::class.qualifiedName

    companion object {
        fun fromRoute(route: String?): HomeNavigationTab {
            if (route == HomeNavRoute.ProfileAuth::class.qualifiedName) return Settings
            if (route == HomeNavRoute.ProfileEdit::class.qualifiedName) return Settings
            if (route == HomeNavRoute.HubLiked::class.qualifiedName || route == HomeNavRoute.HubDetail::class.qualifiedName) return Browser
            return entries.firstOrNull { it.routeName == route } ?: Projects
        }
    }
}
