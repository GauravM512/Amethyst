package dev.anthonyhfm.amethyst.home.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import dev.anthonyhfm.amethyst.home.account.AndroidHubAccount
import dev.anthonyhfm.amethyst.home.nav.HomeNavRoute
import dev.anthonyhfm.amethyst.hub.data.HubHome
import dev.anthonyhfm.amethyst.hub.data.HubProjectSort
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserView(navigator: NavHostController, onOpenProject: (String, String) -> Unit) {
    val context = LocalContext.current
    val account = remember(context) { AndroidHubAccount.get(context) }
    val repository = account.repository
    val scope = rememberCoroutineScope()
    val searchState = rememberSearchBarState()
    val queryState = rememberTextFieldState()
    var browseAll by remember { mutableStateOf(false) }
    var browseSort by remember { mutableStateOf(HubProjectSort.newest) }
    var feed by remember { mutableStateOf<HubHome?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh, account.sessionRevision) {
        loading = true
        error = false
        try { feed = repository.getHome.execute() }
        catch (_: Exception) { error = true }
        finally { loading = false }
    }
    val searchExpanded = searchState.currentValue == SearchBarValue.Expanded
    LaunchedEffect(searchExpanded) { if (!searchExpanded) browseAll = false }

    fun openHref(href: String?) {
        if (href == null) return
        val path = runCatching { URI(href).path }.getOrNull() ?: href.substringBefore('?')
        val parts = path.trim('/').split('/').filter { it.isNotBlank() }
        if (parts.firstOrNull() == "projects") {
            browseAll = true
            browseSort = if (href.contains("sort=popular")) HubProjectSort.popular else HubProjectSort.newest
            scope.launch { searchState.animateToExpanded() }
        } else if (parts.firstOrNull()?.startsWith('@') == true) {
            val username = parts[0].removePrefix("@")
            if (username.isNotBlank()) {
                val slug = parts.getOrNull(1)
                if (slug == null) navigator.navigate(HomeNavRoute.HubDetail(username, null))
                else onOpenProject(username, slug)
            }
        }
    }

    val inputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            textFieldState = queryState,
            searchBarState = searchState,
            onSearch = {},
            placeholder = { Text(stringResource(Res.string.home_hub_search_placeholder)) },
            leadingIcon = {
                if (searchExpanded) IconButton(onClick = { scope.launch { searchState.animateToCollapsed() } }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_cancel))
                } else Icon(Icons.Default.Search, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(Res.string.home_hub_title)) },
                    actions = {
                        IconButton(onClick = { navigator.navigate(HomeNavRoute.HubLiked) }) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = stringResource(Res.string.home_hub_liked_title))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    SearchBar(state = searchState, inputField = inputField, modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth())
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                feed == null && loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                feed == null && error -> HubFeedMessage(
                    stringResource(Res.string.home_hub_error_title),
                    stringResource(Res.string.home_hub_error_generic),
                ) { refresh++ }
                feed?.sections.isNullOrEmpty() -> HubFeedMessage(
                    stringResource(Res.string.home_hub_empty_title),
                    stringResource(Res.string.home_hub_empty_description),
                ) { refresh++ }
                else -> PullToRefreshBox(
                    isRefreshing = loading,
                    onRefresh = { refresh++ },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(30.dp),
                    ) {
                        items(feed!!.sections, key = { it.id }) { section ->
                            HubHomeSectionView(
                                section = section,
                                account = account,
                                onOpenHref = ::openHref,
                                onSignIn = { navigator.navigate(HomeNavRoute.ProfileAuth) },
                            )
                        }
                        item(key = "all_projects") {
                            Button(
                                onClick = { openHref("/projects") },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            ) { Text(stringResource(Res.string.home_hub_show_all_projects)) }
                        }
                    }
                }
            }
        }
    }

    ExpandedFullScreenSearchBar(
        state = searchState,
        inputField = inputField,
        colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        HubSearchContent(
            repository = repository,
            query = queryState.text.toString(),
            browseAll = browseAll,
            initialSort = browseSort,
            onOpenArtist = { username ->
                scope.launch { searchState.animateToCollapsed(); navigator.navigate(HomeNavRoute.HubDetail(username, null)) }
            },
            onOpenProject = { username, slug ->
                scope.launch { searchState.animateToCollapsed(); onOpenProject(username, slug) }
            },
        )
    }
}

@Composable
private fun HubFeedMessage(title: String, description: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(Res.string.home_hub_retry)) }
    }
}
