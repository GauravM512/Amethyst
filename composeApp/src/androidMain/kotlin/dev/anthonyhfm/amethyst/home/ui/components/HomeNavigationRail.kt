package dev.anthonyhfm.amethyst.home.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import dev.anthonyhfm.amethyst.home.nav.HomeNavigationTab

@Composable
fun HomeNavigationRail(
    navigator: NavHostController,
    currentTab: HomeNavigationTab,
) {
    val railItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        HomeNavigationTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = currentTab == tab,
                onClick = {
                    navigator.navigate(tab.route) {
                        popUpTo(navigator.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = railItemColors,
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(tab.label) },
            )
        }
    }
}
