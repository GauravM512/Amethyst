package dev.anthonyhfm.amethyst.home.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.anthonyhfm.amethyst.home.nav.HomeNavigationTab
import dev.anthonyhfm.amethyst.home.ui.components.HomeBottomNavBar
import dev.anthonyhfm.amethyst.home.ui.components.HomeNavigationDrawer
import dev.anthonyhfm.amethyst.home.ui.components.HomeNavigationRail

@Composable
fun AdaptiveHomeNavLayout(
    navigator: NavHostController,
    currentTab: HomeNavigationTab,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        when {
            maxWidth >= 840.dp -> {
                HomeNavigationDrawer(
                    navigator = navigator,
                    currentTab = currentTab,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        content()
                    }
                }
            }

            maxWidth >= 600.dp -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    HomeNavigationRail(
                        navigator = navigator,
                        currentTab = currentTab,
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        content()
                    }
                }
            }

            else -> {
                Scaffold(
                    bottomBar = {
                        HomeBottomNavBar(
                            navigator = navigator,
                            currentTab = currentTab,
                        )
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
