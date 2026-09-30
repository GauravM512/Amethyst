package dev.anthonyhfm.amethyst.home.ui.views

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import dev.anthonyhfm.amethyst.home.account.AndroidHubAccount
import dev.anthonyhfm.amethyst.hub.data.HubArtist
import dev.anthonyhfm.amethyst.hub.data.HubProject
import dev.anthonyhfm.amethyst.hub.data.HubProjectCollection
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
private fun HubArtistCollectionSection(
    collection: HubProjectCollection,
    onOpenProject: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = 12.dp,
                bottom = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = collection.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (collection.description.isNotBlank()) {
                Text(
                    text = collection.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                horizontal = 20.dp
            )
        ) {
            items(
                items = collection.projects,
                key = { project ->
                    "collection_${collection.id}_${project.id}"
                }
            ) { project ->
                Column(
                    modifier = Modifier
                        .width(156.dp)
                        .clickable {
                            onOpenProject(
                                project.artist.username,
                                project.slug
                            )
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HubArtwork(
                        url = project.thumbnailUrl,
                        modifier = Modifier
                            .size(156.dp),
                        shape = MaterialTheme.shapes.large
                    )

                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = project.artist.displayName.ifBlank {
                            "@${project.artist.username}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

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
    var artist by remember(username) {
        mutableStateOf<HubArtist?>(null)
    }
    var artistProjects by remember(username) {
        mutableStateOf<List<HubProject>>(emptyList())
    }
    var collections by remember(username) {
        mutableStateOf<List<HubProjectCollection>>(emptyList())
    }
    var nextCursor by remember(username) {
        mutableStateOf<String?>(null)
    }
    var loading by remember(username) {
        mutableStateOf(true)
    }
    var loadingMore by remember(username) {
        mutableStateOf(false)
    }
    var moreError by remember(username) {
        mutableStateOf(false)
    }
    var busy by remember {
        mutableStateOf(false)
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }
    var reload by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(username, reload, account.sessionRevision) {
        loading = true
        error = null
        artistProjects = emptyList()
        collections = emptyList()
        nextCursor = null
        moreError = false
        try {
            artist = account.repository.getArtist.execute(
                username = username
            )
            collections = runCatching {
                account.repository.getArtistCollections.execute(
                    username = username
                )
            }.getOrDefault(emptyList())
            val page = account.repository.getArtistProjects.execute(
                username = username
            )
            artistProjects = page.items
            nextCursor = page.nextCursor
        } catch (cause: Exception) {
            error = cause.message ?: cause.toString()
        } finally {
            loading = false
        }
    }

    val loadMoreProjects: () -> Unit = {
        val cursor = nextCursor
        if (cursor != null && !loadingMore) {
            scope.launch {
                loadingMore = true
                moreError = false
                try {
                    val page = account.repository.getArtistProjects.execute(
                        username = username,
                        cursor = cursor
                    )
                    val seen = artistProjects.mapTo(mutableSetOf()) { project ->
                        project.id
                    }
                    artistProjects = artistProjects + page.items.filter { project ->
                        seen.add(project.id)
                    }
                    nextCursor = page.nextCursor
                } catch (_: Exception) {
                    moreError = true
                } finally {
                    loadingMore = false
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = artist?.displayName ?: stringResource(Res.string.home_hub_title),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.home_hub_dismiss)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = {
                                reload++
                            }
                        ) {
                            Text(
                                text = stringResource(Res.string.home_hub_retry)
                            )
                        }
                    }
                }
                artist != null -> {
                    val visibleCollections = collections.filter { collection ->
                        collection.projects.isNotEmpty()
                    }

                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = 32.dp
                        ),
                    ) {
                        item(key = "artist_profile_header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HubArtwork(
                                    url = artist!!.avatarUrl,
                                    modifier = Modifier
                                        .size(112.dp),
                                    shape = CircleShape
                                )
                                Text(
                                    text = artist!!.displayName.ifBlank { artist!!.username },
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "@${artist!!.username}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${artist!!.followersCount} ${stringResource(Res.string.home_hub_followers)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (artist!!.bio.isNotBlank()) {
                                    Text(
                                        text = artist!!.bio,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (!account.repository.client.isAuthenticated) {
                                            onSignIn()
                                        } else {
                                            scope.launch {
                                                busy = true
                                                try {
                                                    val result = if (artist!!.isFollowing) {
                                                        account.repository.unfollowArtist.execute(
                                                            username = username
                                                        )
                                                    } else {
                                                        account.repository.followArtist.execute(
                                                            username = username
                                                        )
                                                    }
                                                    artist = artist!!.copy(
                                                        isFollowing = result.following,
                                                        followersCount = result.followersCount
                                                    )
                                                } catch (cause: Exception) {
                                                    error = cause.message ?: cause.toString()
                                                } finally {
                                                    busy = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !busy
                                ) {
                                    Text(
                                        text = stringResource(
                                            if (artist!!.isFollowing) {
                                                Res.string.home_hub_following
                                            } else {
                                                Res.string.home_hub_follow
                                            }
                                        )
                                    )
                                }
                            }
                        }

                        if (visibleCollections.isNotEmpty()) {
                            items(
                                items = visibleCollections,
                                key = { collection ->
                                    "collection_${collection.id}"
                                }
                            ) { collection ->
                                HubArtistCollectionSection(
                                    collection = collection,
                                    onOpenProject = onOpenProject
                                )
                            }
                        }

                        item(key = "projects_heading") {
                            Text(
                                text = stringResource(Res.string.home_hub_detail_projects_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 20.dp,
                                        end = 20.dp,
                                        top = 16.dp,
                                        bottom = 8.dp
                                    )
                            )
                        }

                        if (artistProjects.isEmpty()) {
                            item(key = "projects_empty") {
                                Text(
                                    text = stringResource(Res.string.home_hub_detail_no_projects),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 20.dp,
                                            vertical = 12.dp
                                        )
                                )
                            }
                        } else {
                            items(
                                items = artistProjects,
                                key = { item ->
                                    item.id
                                }
                            ) { item ->
                                HubProjectResult(
                                    project = item,
                                    onClick = {
                                        onOpenProject(
                                            item.artist.username,
                                            item.slug
                                        )
                                    }
                                )
                            }
                        }

                        if (nextCursor != null) {
                            item(key = "projects_sentinel") {
                                LaunchedEffect(nextCursor) {
                                    if (!moreError) {
                                        loadMoreProjects()
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (loadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else if (moreError) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.home_hub_detail_projects_error),
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall
                                            )

                                            TextButton(
                                                onClick = {
                                                    loadMoreProjects()
                                                }
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.home_hub_retry)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (artistProjects.isNotEmpty()) {
                            item(key = "projects_end_indicator") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 24.dp
                                        ),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        8.dp,
                                        Alignment.CenterHorizontally
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Text(
                                        text = "${artistProjects.size} ${stringResource(Res.string.home_hub_detail_projects)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
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
