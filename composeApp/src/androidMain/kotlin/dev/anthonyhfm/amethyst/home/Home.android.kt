package dev.anthonyhfm.amethyst.home

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.anthonyhfm.amethyst.home.nav.HomeNavRoute
import dev.anthonyhfm.amethyst.home.nav.HomeNavigationTab
import dev.anthonyhfm.amethyst.home.account.AndroidHubAccount
import dev.anthonyhfm.amethyst.home.ui.layout.AdaptiveHomeNavLayout
import dev.anthonyhfm.amethyst.home.ui.views.AbletonImportWizardSheet
import dev.anthonyhfm.amethyst.home.ui.views.ArcadeView
import dev.anthonyhfm.amethyst.home.ui.views.BrowserView
import dev.anthonyhfm.amethyst.home.ui.views.LoadingScreenView
import dev.anthonyhfm.amethyst.home.ui.views.ProjectsView
import dev.anthonyhfm.amethyst.home.ui.views.SettingsView
import dev.anthonyhfm.amethyst.home.ui.views.AuthScreen
import dev.anthonyhfm.amethyst.home.ui.views.EditProfileScreen
import dev.anthonyhfm.amethyst.home.ui.views.HubDetailScreen
import dev.anthonyhfm.amethyst.home.ui.views.HubLikedScreen
import dev.anthonyhfm.amethyst.home.ui.views.HubProjectSheet

@Composable
actual fun Home(
    onOpenWorkspace: () -> Unit,
) {
    val navigator = rememberNavController()
    val currentBackStackEntry by navigator.currentBackStackEntryAsState()
    val currentTab = HomeNavigationTab.fromRoute(currentBackStackEntry?.destination?.route)
    var selectedProject by remember { mutableStateOf<Pair<String, String>?>(null) }

    AdaptiveHomeNavLayout(
        navigator = navigator,
        currentTab = currentTab,
    ) {
        NavHost(
            navController = navigator,
            startDestination = HomeNavRoute.Projects,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable<HomeNavRoute.Projects> {
                ProjectsView(
                    navigator = navigator,
                    onOpenWorkspace = onOpenWorkspace,
                )
            }

            composable<HomeNavRoute.Browser> {
                BrowserView(navigator, onOpenProject = { username, slug -> selectedProject = username to slug })
            }

            dialog<HomeNavRoute.HubLiked>(
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            ) {
                HubLikedScreen(
                    account = AndroidHubAccount.get(LocalContext.current),
                    onClose = { navigator.popBackStack() },
                    onSignIn = { navigator.navigate(HomeNavRoute.ProfileAuth) },
                    onOpenProject = { username, slug -> selectedProject = username to slug },
                )
            }

            dialog<HomeNavRoute.HubDetail>(
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            ) {
                val route = it.toRoute<HomeNavRoute.HubDetail>()
                if (route.slug != null) {
                    LaunchedEffect(route.username, route.slug) {
                        navigator.popBackStack()
                        selectedProject = route.username to route.slug
                    }
                } else {
                    HubDetailScreen(
                        account = AndroidHubAccount.get(LocalContext.current),
                        username = route.username,
                        onClose = { navigator.popBackStack() },
                        onSignIn = { navigator.navigate(HomeNavRoute.ProfileAuth) },
                        onOpenProject = { username, slug -> selectedProject = username to slug },
                    )
                }
            }

            composable<HomeNavRoute.Arcade> {
                ArcadeView()
            }

            composable<HomeNavRoute.Settings> {
                SettingsView(
                    onRequestAuth = { navigator.navigate(HomeNavRoute.ProfileAuth) },
                    onRequestEditProfile = { navigator.navigate(HomeNavRoute.ProfileEdit) },
                )
            }

            dialog<HomeNavRoute.ProfileAuth>(
                dialogProperties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                    dismissOnClickOutside = false,
                ),
            ) {
                val account = AndroidHubAccount.get(LocalContext.current)
                LaunchedEffect(account.account) {
                    if (account.account != null) navigator.popBackStack()
                }
                AuthScreen(account, onDismiss = { navigator.popBackStack() })
            }

            dialog<HomeNavRoute.ProfileEdit>(
                dialogProperties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                    dismissOnClickOutside = false,
                ),
            ) {
                val account = AndroidHubAccount.get(LocalContext.current)
                LaunchedEffect(account.account) {
                    if (account.account == null) navigator.popBackStack()
                }
                EditProfileScreen(account, onDismiss = { navigator.popBackStack() })
            }

            dialog<HomeNavRoute.AbletonImportWizard>(
                dialogProperties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                ),
            ) {
                val route = it.toRoute<HomeNavRoute.AbletonImportWizard>()
                AbletonImportWizardSheet(
                    path = route.liveSetPath,
                    navigator = navigator,
                    onOpenWorkspace = onOpenWorkspace,
                )
            }

            dialog<HomeNavRoute.LoadingScreen>(
                dialogProperties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false,
                ),
            ) {
                val route = it.toRoute<HomeNavRoute.LoadingScreen>()
                LoadingScreenView(message = route.text)
            }
        }
    }

    selectedProject?.let { (username, slug) ->
        HubProjectSheet(
            account = AndroidHubAccount.get(LocalContext.current),
            username = username,
            slug = slug,
            onClose = { selectedProject = null },
            onSignIn = { selectedProject = null; navigator.navigate(HomeNavRoute.ProfileAuth) },
            onOpenArtist = { artist -> selectedProject = null; navigator.navigate(HomeNavRoute.HubDetail(artist, null)) },
            onOpenWorkspace = onOpenWorkspace,
        )
    }
}
