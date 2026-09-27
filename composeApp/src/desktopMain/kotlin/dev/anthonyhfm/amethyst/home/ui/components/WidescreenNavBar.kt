package dev.anthonyhfm.amethyst.home.ui.components

import amethyst.composeapp.generated.resources.*
import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.amethyst_studio_logo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.composeunstyled.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.composables.icons.lucide.BadgeInfo
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Gamepad2
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.History
import com.composables.icons.lucide.House
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.UserRound
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.home.nav.HomeNavRoute
import dev.anthonyhfm.amethyst.home.ui.views.DesktopHubSection
import dev.anthonyhfm.amethyst.home.account.DesktopHubAccount
import dev.anthonyhfm.amethyst.home.ui.views.DesktopHubAvatar
import dev.anthonyhfm.amethyst.ui.components.primitives.LocalSidebarState
import dev.anthonyhfm.amethyst.ui.components.primitives.Sidebar
import dev.anthonyhfm.amethyst.ui.components.primitives.SidebarContent
import dev.anthonyhfm.amethyst.ui.components.primitives.SidebarFooter
import dev.anthonyhfm.amethyst.ui.components.primitives.SidebarGroup
import dev.anthonyhfm.amethyst.ui.components.primitives.SidebarGroupContent
import dev.anthonyhfm.amethyst.ui.components.primitives.SidebarGroupLabel
import dev.anthonyhfm.amethyst.ui.components.primitives.SidebarHeader
import dev.anthonyhfm.amethyst.ui.components.primitives.SidebarMenu
import dev.anthonyhfm.amethyst.ui.components.primitives.SidebarMenuButton
import dev.anthonyhfm.amethyst.ui.components.primitives.SidebarMenuItem
import dev.anthonyhfm.amethyst.ui.components.primitives.SidebarSeparator
import dev.anthonyhfm.amethyst.ui.components.primitives.SidebarTrigger
import dev.anthonyhfm.amethyst.ui.theme.accentForeground
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.foreground
import dev.anthonyhfm.amethyst.ui.theme.large
import dev.anthonyhfm.amethyst.ui.theme.mutedForeground
import dev.anthonyhfm.amethyst.ui.theme.small
import dev.anthonyhfm.amethyst.ui.theme.typography
import dev.anthonyhfm.amethyst.ui.icons.AmethystIcons
import dev.anthonyhfm.amethyst.ui.icons.filled.Saddam
import dev.nucleusframework.core.runtime.ExecutableRuntime
import org.jetbrains.compose.resources.painterResource

@Composable
fun WidescreenNavBar(
    navigator: NavHostController,
    hubSection: DesktopHubSection,
    onHubSectionChange: (DesktopHubSection) -> Unit,
) {
    val current by navigator.currentBackStackEntryAsState()
    var currentNavigation: HomeNavRoute by remember { mutableStateOf(HomeNavRoute.Recent) }
    var sidebarToggleCount by remember { mutableStateOf(0) }
    var hasObservedInitialSidebarState by remember { mutableStateOf(false) }
    val sidebarState = LocalSidebarState.current
    val hubAccount = remember { DesktopHubAccount.get() }
    val account = hubAccount.account

    LaunchedEffect(sidebarState.expanded) {
        if (hasObservedInitialSidebarState) {
            sidebarToggleCount++
        } else {
            hasObservedInitialSidebarState = true
        }
    }

    LaunchedEffect(current) {
        currentNavigation = when (current?.destination?.route) {
            HomeNavRoute.Recent::class.qualifiedName -> HomeNavRoute.Recent
            HomeNavRoute.Browser::class.qualifiedName -> HomeNavRoute.Browser
            HomeNavRoute.Arcade::class.qualifiedName -> HomeNavRoute.Arcade
            HomeNavRoute.Settings::class.qualifiedName -> HomeNavRoute.Settings
            HomeNavRoute.Account::class.qualifiedName -> HomeNavRoute.Account
            HomeNavRoute.Tutorials::class.qualifiedName -> HomeNavRoute.Tutorials
            HomeNavRoute.About::class.qualifiedName -> HomeNavRoute.About

            else -> currentNavigation
        }
    }

    val primaryItems = mutableListOf(NavRailItem.RECENT)

    primaryItems.add(NavRailItem.BROWSER)
    primaryItems.add(NavRailItem.ARCADE)

    val secondaryItems = listOf(
        NavRailItem.TUTORIALS,
        NavRailItem.SETTINGS,
        NavRailItem.ACCOUNT,
        NavRailItem.ABOUT,
    )

    Sidebar {
        SidebarHeader {
            if (sidebarState.expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                        SidebarBrandMark(size = 42.dp)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Amethyst",
                                style = Theme[typography][large].copy(color = Theme[colors][foreground]),
                            )
                            Text(
                                text = "Studio Home",
                                style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]),
                            )
                        }
                    Spacer(Modifier.weight(1f))
                    SidebarTrigger()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SidebarBrandMark(size = 24.dp)
                    SidebarTrigger(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    )
                }
            }
        }

        SidebarSeparator()

        SidebarContent(
            modifier = Modifier
                .padding(bottom = 12.dp),
        ) {
            SidebarGroup {
                SidebarGroupLabel("Home")
                SidebarGroupContent {
                    SidebarMenu {
                        primaryItems.forEach { item ->
                            SidebarMenuItem {
                                SidebarMenuButton(
                                    onClick = {
                                        if (item.route == HomeNavRoute.Browser) onHubSectionChange(DesktopHubSection.Home)
                                        if (currentNavigation != item.route) {
                                            navigator.navigate(item.route) {
                                                launchSingleTop = true
                                                popUpTo(navigator.graph.findStartDestination().id)
                                            }
                                        }
                                    },
                                    isActive = currentNavigation == item.route && (item.route != HomeNavRoute.Browser || hubSection == DesktopHubSection.Search),
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (currentNavigation == item.route) {
                                                Theme[colors][accentForeground]
                                            } else {
                                                Theme[colors][foreground].copy(alpha = 0.75f)
                                            },
                                        )
                                    },
                                ) {
                                    Text(item.expandedLabel)
                                }
                            }
                            if (item.route == HomeNavRoute.Browser && currentNavigation == HomeNavRoute.Browser) {
                                listOf(
                                    Triple(DesktopHubSection.Home, Res.string.home_widescreen_navbar_group_home, Lucide.House),
                                    Triple(DesktopHubSection.Liked, Res.string.home_hub_liked_title, Lucide.Heart),
                                    Triple(DesktopHubSection.Projects, Res.string.home_hub_search_projects, Lucide.LayoutGrid),
                                ).forEach { (section, label, icon) ->
                                    SidebarMenuItem {
                                        SidebarMenuButton(
                                            onClick = { onHubSectionChange(section) },
                                            modifier = Modifier.padding(start = if (sidebarState.expanded) 18.dp else 0.dp),
                                            isActive = hubSection == section,
                                            icon = {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(17.dp),
                                                    tint = if (hubSection == section) Theme[colors][accentForeground] else Theme[colors][foreground].copy(alpha = 0.7f),
                                                )
                                            },
                                        ) { Text(stringResource(label)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        if (sidebarToggleCount >= 6) {
            Icon(
                imageVector = AmethystIcons.Filled.Saddam,
                contentDescription = null,
                tint = Theme[colors][foreground].copy(alpha = 0.08f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 100.dp, height = 24.dp),
            )
        }

        SidebarSeparator(modifier = Modifier.alpha(0.75f))

        SidebarFooter {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                SidebarGroupLabel("Support")
                SidebarGroupContent {
                    SidebarMenu {
                        secondaryItems.forEach { item ->
                            SidebarMenuItem {
                                SidebarMenuButton(
                                    onClick = {
                                        if (currentNavigation != item.route) {
                                            navigator.navigate(item.route) {
                                                launchSingleTop = true
                                                popUpTo(navigator.graph.findStartDestination().id)
                                            }
                                        }
                                    },
                                    isActive = currentNavigation == item.route,
                                    icon = {
                                        if (item.route == HomeNavRoute.Account) {
                                            DesktopHubAvatar(
                                                username = account?.username.orEmpty(),
                                                avatarUrl = account?.avatarUrl,
                                                size = 20.dp,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = if (currentNavigation == item.route) {
                                                    Theme[colors][accentForeground]
                                                } else {
                                                    Theme[colors][foreground].copy(alpha = 0.75f)
                                                },
                                            )
                                        }
                                    },
                                ) {
                                    if (item.route == HomeNavRoute.Account && account != null) {
                                        Text(
                                            "${account.displayName.ifBlank { account.username }} · @${account.username}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    } else {
                                        Text(item.expandedLabel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class NavRailItem(
    val labelRes: StringResource,
    val expandedLabelRes: StringResource = labelRes,
    val icon: ImageVector,
    val route: HomeNavRoute
) {
    val label: String @Composable get() = stringResource(labelRes)
    val expandedLabel: String @Composable get() = stringResource(expandedLabelRes)

    companion object {
        val RECENT = NavRailItem(
            labelRes = Res.string.home_widescreen_navbar_recent,
            expandedLabelRes = Res.string.home_widescreen_navbar_recent_projects,
            icon = Lucide.History,
            route = HomeNavRoute.Recent
        )

        val BROWSER = NavRailItem(
            labelRes = Res.string.home_hub_title,
            icon = Lucide.Globe,
            route = HomeNavRoute.Browser
        )

        val ARCADE = NavRailItem(
            labelRes = Res.string.home_widescreen_navbar_arcade,
            expandedLabelRes = Res.string.home_widescreen_navbar_arcade,
            icon = Lucide.Gamepad2,
            route = HomeNavRoute.Arcade
        )

        val SETTINGS = NavRailItem(
            labelRes = Res.string.home_widescreen_navbar_settings,
            icon = Lucide.Settings2,
            route = HomeNavRoute.Settings
        )

        val ACCOUNT = NavRailItem(
            labelRes = Res.string.account_section_title,
            icon = Lucide.UserRound,
            route = HomeNavRoute.Account
        )

        val TUTORIALS = NavRailItem(
            labelRes = Res.string.home_widescreen_navbar_tutorials,
            expandedLabelRes = Res.string.home_widescreen_navbar_tutorials,
            icon = Lucide.BookOpen,
            route = HomeNavRoute.Tutorials
        )

        val ABOUT = NavRailItem(
            labelRes = Res.string.home_widescreen_navbar_about,
            expandedLabelRes = Res.string.home_widescreen_navbar_about_amethyst,
            icon = Lucide.BadgeInfo,
            route = HomeNavRoute.About
        )
    }
}

@Composable
private fun SidebarBrandMark(
    size: androidx.compose.ui.unit.Dp,
) {
    Image(
        painter = painterResource(Res.drawable.amethyst_studio_logo),
        contentDescription = "Amethyst Logo",
        modifier = Modifier.size(size),
    )
}
