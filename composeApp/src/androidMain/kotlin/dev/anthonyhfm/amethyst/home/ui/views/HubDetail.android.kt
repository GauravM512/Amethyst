package dev.anthonyhfm.amethyst.home.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import dev.anthonyhfm.amethyst.home.account.AndroidHubAccount
import dev.anthonyhfm.amethyst.hub.data.HubArtist
import dev.anthonyhfm.amethyst.hub.data.HubProject
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HubDetailScreen(
    account: AndroidHubAccount,
    username: String,
    onClose: () -> Unit,
    onSignIn: () -> Unit,
    onOpenProject: (String, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var artist by remember(username) { mutableStateOf<HubArtist?>(null) }
    var artistProjects by remember(username) { mutableStateOf<List<HubProject>>(emptyList()) }
    var loading by remember(username) { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(username, reload, account.sessionRevision) {
        loading = true
        error = null
        try {
            artist = account.repository.getArtist.execute(username)
            artistProjects = account.repository.getArtistProjects.execute(username).items
        } catch (cause: Exception) { error = cause.message ?: cause.toString() }
        finally { loading = false }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(artist?.displayName ?: stringResource(Res.string.home_hub_title), maxLines = 1) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.home_hub_dismiss)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                error != null -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { reload++ }) { Text(stringResource(Res.string.home_hub_retry)) }
                }
                artist != null -> LazyColumn(
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HubArtwork(artist!!.avatarUrl, Modifier.size(112.dp), CircleShape)
                            Text(artist!!.displayName.ifBlank { artist!!.username }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("@${artist!!.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${artist!!.followersCount} ${stringResource(Res.string.home_hub_followers)}", style = MaterialTheme.typography.bodySmall)
                            if (artist!!.bio.isNotBlank()) Text(artist!!.bio, style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = {
                                if (!account.repository.client.isAuthenticated) onSignIn()
                                else scope.launch {
                                    busy = true
                                    try {
                                        val result = if (artist!!.isFollowing) account.repository.unfollowArtist.execute(username)
                                        else account.repository.followArtist.execute(username)
                                        artist = artist!!.copy(isFollowing = result.following, followersCount = result.followersCount)
                                    } catch (cause: Exception) { error = cause.message ?: cause.toString() }
                                    finally { busy = false }
                                }
                            }, enabled = !busy) { Text(stringResource(if (artist!!.isFollowing) Res.string.home_hub_following else Res.string.home_hub_follow)) }
                        }
                    }
                    item { Text(stringResource(Res.string.home_hub_detail_projects_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(20.dp)) }
                    items(artistProjects, key = { it.id }) { item -> HubProjectResult(item) { onOpenProject(item.artist.username, item.slug) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HubLikedScreen(
    account: AndroidHubAccount,
    onClose: () -> Unit,
    onSignIn: () -> Unit,
    onOpenProject: (String, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var projects by remember { mutableStateOf<List<HubProject>>(emptyList()) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(account.sessionRevision, reload) {
        if (!account.repository.client.isAuthenticated) return@LaunchedEffect
        loading = true
        try {
            val page = account.repository.getLikedProjects.execute()
            projects = page.items
            cursor = page.nextCursor
            error = false
        } catch (_: Exception) { error = true }
        finally { loading = false }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(
            title = { Text(stringResource(Res.string.home_hub_liked_title)) },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.home_hub_dismiss)) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        ) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                !account.repository.client.isAuthenticated -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(Res.string.home_hub_liked_sign_in))
                    Button(onClick = onSignIn) { Text(stringResource(Res.string.home_hub_sign_in)) }
                }
                loading && projects.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                error && projects.isEmpty() -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(Res.string.home_hub_liked_error))
                    Button(onClick = { reload++ }) { Text(stringResource(Res.string.home_hub_retry)) }
                }
                projects.isEmpty() -> Text(stringResource(Res.string.home_hub_liked_empty), modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                    items(projects, key = { it.id }) { item -> HubProjectResult(item) { onOpenProject(item.artist.username, item.slug) } }
                    if (cursor != null) item {
                        TextButton(onClick = {
                            val next = cursor ?: return@TextButton
                            scope.launch {
                                loading = true
                                try {
                                    val page = account.repository.getLikedProjects.execute(cursor = next)
                                    val seen = projects.mapTo(mutableSetOf()) { it.id }
                                    projects = projects + page.items.filter { seen.add(it.id) }
                                    cursor = page.nextCursor
                                    error = false
                                } catch (_: Exception) { error = true }
                                finally { loading = false }
                            }
                        }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(if (error) Res.string.home_hub_retry else Res.string.home_hub_detail_load_more))
                        }
                    }
                }
            }
        }
    }
}
