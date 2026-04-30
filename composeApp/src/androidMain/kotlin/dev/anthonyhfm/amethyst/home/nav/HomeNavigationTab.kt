package dev.anthonyhfm.amethyst.home.nav

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings2

enum class HomeNavigationTab(
    val label: String,
    val icon: ImageVector,
    val route: HomeNavRoute,
) {
    Projects(
        label = "Projects",
        icon = Lucide.History,
        route = HomeNavRoute.Projects,
    ),
    Browser(
        label = "Browser",
        icon = Lucide.FolderOpen,
        route = HomeNavRoute.Browser,
    ),
    Settings(
        label = "Settings",
        icon = Lucide.Settings2,
        route = HomeNavRoute.Settings,
    );

    val routeName: String?
        get() = route::class.qualifiedName

    companion object {
        fun fromRoute(route: String?): HomeNavigationTab {
            return entries.firstOrNull { it.routeName == route } ?: Projects
        }
    }
}
