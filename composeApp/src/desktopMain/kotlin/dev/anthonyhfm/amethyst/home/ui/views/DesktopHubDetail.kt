package dev.anthonyhfm.amethyst.home.ui.views

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.Icon
import com.composeunstyled.theme.Theme
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.StarHalf
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Lucide
import dev.anthonyhfm.amethyst.hub.data.HubArtist
import dev.anthonyhfm.amethyst.hub.data.HubProject
import dev.anthonyhfm.amethyst.hub.data.HubProjectCollection
import dev.anthonyhfm.amethyst.hub.data.HubRepository
import dev.anthonyhfm.amethyst.ui.components.primitives.Spinner
import dev.anthonyhfm.amethyst.ui.theme.border
import dev.anthonyhfm.amethyst.ui.theme.card
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.foreground
import dev.anthonyhfm.amethyst.ui.theme.h2
import dev.anthonyhfm.amethyst.ui.theme.h3
import dev.anthonyhfm.amethyst.ui.theme.mutedForeground
import dev.anthonyhfm.amethyst.ui.theme.p
import dev.anthonyhfm.amethyst.ui.theme.small
import dev.anthonyhfm.amethyst.ui.theme.primary
import dev.anthonyhfm.amethyst.ui.theme.primaryForeground
import dev.anthonyhfm.amethyst.ui.theme.typography
import dev.anthonyhfm.amethyst.ui.components.primitives.Button
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonSize
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonVariant
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed interface DesktopHubDestination {
    data class Artist(val username: String) : DesktopHubDestination
    data class Project(val username: String, val slug: String) : DesktopHubDestination
}

@Composable
fun DesktopHubDetail(
    destination: DesktopHubDestination,
    repository: HubRepository,
    onBack: () -> Unit,
    onNavigate: (DesktopHubDestination) -> Unit,
    onSignIn: () -> Unit,
    onOpenDownloadedFile: suspend (File, HubProject) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        is DesktopHubDestination.Artist -> {
            DesktopHubArtistDetail(
                username = destination.username,
                repository = repository,
                onBack = onBack,
                onNavigate = onNavigate,
                onSignIn = onSignIn,
                modifier = modifier,
            )
        }

        is DesktopHubDestination.Project -> {
            DesktopHubProjectDetail(
                username = destination.username,
                slug = destination.slug,
                repository = repository,
                onBack = onBack,
                onNavigate = onNavigate,
                onSignIn = onSignIn,
                onOpenDownloadedFile = onOpenDownloadedFile,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun DesktopHubArtistDetail(
    username: String,
    repository: HubRepository,
    onBack: () -> Unit,
    onNavigate: (DesktopHubDestination) -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var artist by remember(username) { mutableStateOf<HubArtist?>(null) }
    var projects by remember(username) { mutableStateOf(emptyList<HubProject>()) }
    var collections by remember(username) { mutableStateOf(emptyList<HubProjectCollection>()) }
    var cursor by remember(username) { mutableStateOf<String?>(null) }
    var loading by remember(username) { mutableStateOf(true) }
    var loadingMore by remember(username) { mutableStateOf(false) }
    var following by remember(username) { mutableStateOf(false) }
    var followers by remember(username) { mutableStateOf(0L) }
    var followPending by remember(username) { mutableStateOf(false) }
    var error by remember(username) { mutableStateOf<String?>(null) }
    var loadMoreError by remember(username) { mutableStateOf<String?>(null) }
    var actionError by remember(username) { mutableStateOf<String?>(null) }

    suspend fun loadProjects(next: String?) {
        if (loadingMore) {
            return
        }

        loadingMore = true
        loadMoreError = null

        try {
            val page = repository.getArtistProjects.execute(
                username = username,
                cursor = next,
                limit = 24,
            )
            projects = if (next == null) {
                page.items
            } else {
                projects + page.items.filter { incoming ->
                    projects.none { it.id == incoming.id }
                }
            }
            cursor = page.nextCursor
        } catch (_: Exception) {
            if (next == null) {
                actionError = getString(Res.string.home_hub_detail_projects_error)
            } else {
                loadMoreError = getString(Res.string.home_hub_detail_projects_error)
            }
        } finally {
            loadingMore = false
        }
    }

    suspend fun load() {
        loading = true
        error = null
        loadMoreError = null

        try {
            val fetched = repository.getArtist.execute(username)
            artist = fetched
            following = fetched.isFollowing
            followers = fetched.followersCount
            projects = emptyList()
            cursor = null
            collections = runCatching {
                repository.getArtistCollections.execute(username)
            }.getOrDefault(emptyList()).filter { it.projects.isNotEmpty() }
            loadProjects(null)
        } catch (_: Exception) {
            error = getString(Res.string.home_hub_detail_artist_error)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(username, repository) {
        load()
    }

    LaunchedEffect(scrollState, cursor, loadingMore, loadMoreError, loading) {
        snapshotFlow { scrollState.value to scrollState.maxValue }
            .collect { (value, maxValue) ->
                if (!loading && projects.isNotEmpty() && cursor != null && !loadingMore && loadMoreError == null) {
                    if (maxValue == 0 || (maxValue - value) <= 600) {
                        loadProjects(cursor)
                    }
                }
            }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
    ) {
        val sidePadding = if (maxWidth < 600.dp) 24.dp else 32.dp
        val contentWidth = (maxWidth - sidePadding * 2).coerceAtMost(1200.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .width(contentWidth)
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        top = 28.dp,
                        bottom = 40.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                DesktopHubBack(
                    onClick = onBack
                )

                if (loading && artist == null) {
                    DesktopHubMessage(
                        message = stringResource(Res.string.home_hub_detail_loading)
                    )
                } else if (artist == null) {
                    DesktopHubRetry(
                        message = error ?: stringResource(Res.string.home_hub_detail_artist_unavailable),
                        retry = {
                            scope.launch {
                                load()
                            }
                        }
                    )
                } else {
                    val value = artist!!

                    Text(
                        text = stringResource(Res.string.home_hub_detail_artist_title),
                        style = Theme[typography][h2].copy(color = Theme[colors][foreground])
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        val wide = maxWidth >= 680.dp
                        val avatarSize = if (wide) 144.dp else 84.dp

                        val followButton: @Composable () -> Unit = {
                            DesktopHubButton(
                                label = if (following) {
                                    stringResource(Res.string.home_hub_following)
                                } else {
                                    stringResource(Res.string.home_hub_follow)
                                },
                                icon = if (following) Lucide.Check else Lucide.Plus,
                                enabled = !followPending,
                                onClick = {
                                    if (!repository.client.isAuthenticated) {
                                        onSignIn()
                                        return@DesktopHubButton
                                    }

                                    scope.launch {
                                        followPending = true
                                        try {
                                            val result = if (following) {
                                                repository.unfollowArtist.execute(username)
                                            } else {
                                                repository.followArtist.execute(username)
                                            }
                                            following = result.following
                                            followers = result.followersCount
                                        } catch (_: Exception) {
                                            actionError = getString(Res.string.home_hub_follow_error)
                                        } finally {
                                            followPending = false
                                        }
                                    }
                                }
                            )
                        }

                        val shareButton: @Composable () -> Unit = {
                            DesktopHubButton(
                                label = stringResource(Res.string.home_hub_detail_share),
                                filled = false,
                                icon = Lucide.Share2,
                                onClick = {
                                    DesktopHubDownload.openExternal("https://projects.launchpadders.com/@$username")
                                }
                            )
                        }

                        val identity: @Composable () -> Unit = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = value.displayName.ifBlank { value.username },
                                    style = Theme[typography][if (wide) h2 else h3].copy(color = Theme[colors][foreground]),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                Text(
                                    text = "@${value.username}",
                                    style = Theme[typography][p].copy(color = Theme[colors][mutedForeground]),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                Text(
                                    text = "${value.publishedProjectCount} ${stringResource(Res.string.home_hub_detail_projects)}  ·  $followers ${stringResource(Res.string.home_hub_followers)}",
                                    style = Theme[typography][p].copy(color = Theme[colors][mutedForeground])
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (wide) 32.dp else 18.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            DesktopHubArtwork(
                                url = value.avatarUrl?.let(repository.client::resolveUrl),
                                modifier = Modifier
                                    .size(avatarSize)
                                    .clip(CircleShape),
                                fallback = "◉",
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                identity()

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    followButton()
                                    shareButton()
                                }
                            }
                        }
                    }

                    if (value.bio.isNotBlank()) {
                        Text(
                            text = value.bio,
                            style = Theme[typography][p].copy(color = Theme[colors][foreground])
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Theme[colors][border])
                    )

                    if (collections.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            collections.forEach { collection ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = collection.title,
                                            style = Theme[typography][h3].copy(color = Theme[colors][foreground])
                                        )

                                        if (collection.description.isNotBlank()) {
                                            Text(
                                                text = collection.description,
                                                style = Theme[typography][p].copy(color = Theme[colors][mutedForeground])
                                            )
                                        }
                                    }

                                    DesktopHubRow(
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        collection.projects.forEach { project ->
                                            Column(
                                                modifier = Modifier
                                                    .width(320.dp)
                                                    .clickable {
                                                        onNavigate(
                                                            DesktopHubDestination.Project(
                                                                username = project.artist.username,
                                                                slug = project.slug
                                                            )
                                                        )
                                                    },
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                DesktopHubArtwork(
                                                    url = project.thumbnailUrl?.let(repository.client::resolveUrl),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(16f / 9f)
                                                        .clip(RoundedCornerShape(20.dp))
                                                )

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                    ) {
                                                        Text(
                                                            text = project.artist.displayName.ifBlank { project.artist.username },
                                                            style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]),
                                                            maxLines = 1,
                                                        )

                                                        Text(
                                                            text = project.title,
                                                            style = Theme[typography][p].copy(color = Theme[colors][foreground]),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                    }

                                                    Button(
                                                        onClick = {
                                                            onNavigate(
                                                                DesktopHubDestination.Project(
                                                                    username = project.artist.username,
                                                                    slug = project.slug
                                                                )
                                                            )
                                                        },
                                                        variant = ButtonVariant.Secondary,
                                                        size = ButtonSize.Icon,
                                                        shape = CircleShape,
                                                    ) {
                                                        Icon(
                                                            imageVector = Lucide.ArrowRight,
                                                            contentDescription = project.title,
                                                            modifier = Modifier
                                                                .size(19.dp),
                                                            tint = Theme[colors][foreground],
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Theme[colors][border])
                        )
                    }

                    Text(
                        text = stringResource(Res.string.home_hub_detail_projects_title),
                        style = Theme[typography][h3].copy(color = Theme[colors][foreground])
                    )

                    if (projects.isEmpty()) {
                        DesktopHubMessage(
                            message = stringResource(Res.string.home_hub_detail_no_projects)
                        )
                    } else {
                        DesktopHubProjectGrid(
                            projects = projects,
                            repository = repository,
                            onOpenProject = {
                                onNavigate(DesktopHubDestination.Project(username, it.slug))
                            },
                            onOpenArtist = {
                                onNavigate(DesktopHubDestination.Artist(it))
                            },
                            onSignIn = onSignIn,
                        )
                    }

                    if (loadingMore) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Spinner(
                                size = 24.dp
                            )
                        }
                    }

                    if (loadMoreError != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = loadMoreError!!,
                                style = Theme[typography][small].copy(color = Theme[colors][mutedForeground])
                            )

                            DesktopHubButton(
                                label = stringResource(Res.string.home_hub_retry),
                                filled = false,
                                onClick = {
                                    loadMoreError = null
                                    val next = cursor
                                    if (next != null) {
                                        scope.launch {
                                            loadProjects(next)
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (actionError != null) {
                        DesktopHubRetry(
                            message = actionError!!,
                            retry = {
                                actionError = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopHubProjectDetail(
    username: String,
    slug: String,
    repository: HubRepository,
    onBack: () -> Unit,
    onNavigate: (DesktopHubDestination) -> Unit,
    onSignIn: () -> Unit,
    onOpenDownloadedFile: suspend (File, HubProject) -> Unit,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    var project by remember(username, slug) { mutableStateOf<HubProject?>(null) }
    var loading by remember(username, slug) { mutableStateOf(true) }
    var error by remember(username, slug) { mutableStateOf<String?>(null) }
    var actionError by remember(username, slug) { mutableStateOf<String?>(null) }
    var liked by remember(username, slug) { mutableStateOf(false) }
    var likes by remember(username, slug) { mutableStateOf(0L) }
    var likePending by remember(username, slug) { mutableStateOf(false) }
    var linkCopied by remember(username, slug) { mutableStateOf(false) }
    var downloading by remember(username, slug) { mutableStateOf(false) }
    var progress by remember(username, slug) { mutableStateOf(0f) }

    suspend fun load() {
        loading = true
        error = null

        try {
            val fetched = repository.getPublishedProject.execute(username, slug)
            project = fetched
            liked = fetched.isLiked
            likes = fetched.likesCount
        } catch (_: Exception) {
            error = getString(Res.string.home_hub_detail_project_error)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(username, slug, repository) {
        load()
    }

    LaunchedEffect(linkCopied) {
        if (linkCopied) {
            delay(2000)
            linkCopied = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
    ) {
        val sidePadding = if (maxWidth < 600.dp) 24.dp else 32.dp
        val contentWidth = (maxWidth - sidePadding * 2).coerceAtMost(1140.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = sidePadding,
                        vertical = 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DesktopHubBack(
                    onClick = onBack
                )

                Spacer(
                    modifier = Modifier
                        .weight(1f)
                )

                project?.let { current ->
                    Button(
                        onClick = {
                            if (!repository.client.isAuthenticated) {
                                onSignIn()
                                return@Button
                            }

                            scope.launch {
                                likePending = true
                                try {
                                    val result = repository.toggleProjectLike.execute(current.id)
                                    liked = result.liked
                                    likes = result.likesCount
                                } catch (_: Exception) {
                                    actionError = getString(Res.string.home_hub_detail_like_error)
                                } finally {
                                    likePending = false
                                }
                            }
                        },
                        enabled = !likePending,
                        variant = if (liked) {
                            ButtonVariant.Secondary
                        } else {
                            ButtonVariant.Ghost
                        },
                        size = ButtonSize.Icon,
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Lucide.Heart,
                            contentDescription = stringResource(
                                if (liked) {
                                    Res.string.home_hub_detail_unlike
                                } else {
                                    Res.string.home_hub_detail_like
                                }
                            ),
                            modifier = Modifier
                                .size(19.dp),
                            tint = if (liked) {
                                Theme[colors][primary]
                            } else {
                                Theme[colors][foreground]
                            },
                        )
                    }

                    Button(
                        onClick = {
                            runCatching {
                                Toolkit.getDefaultToolkit().systemClipboard.setContents(
                                    StringSelection("https://projects.launchpadders.com/@$username/$slug"),
                                    null,
                                )
                            }.onSuccess {
                                linkCopied = true
                            }
                        },
                        variant = ButtonVariant.Ghost,
                        size = ButtonSize.Icon,
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = if (linkCopied) {
                                Lucide.Check
                            } else {
                                Lucide.Share2
                            },
                            contentDescription = stringResource(Res.string.home_hub_detail_share),
                            modifier = Modifier
                                .size(19.dp),
                            tint = Theme[colors][foreground],
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .width(contentWidth)
                        .align(Alignment.CenterHorizontally)
                        .padding(
                            top = 20.dp,
                            bottom = 48.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    if (loading && project == null) {
                        DesktopHubMessage(
                            message = stringResource(Res.string.home_hub_detail_loading)
                        )
                    } else if (project == null) {
                        DesktopHubRetry(
                            message = error ?: stringResource(Res.string.home_hub_detail_project_unavailable),
                            retry = {
                                scope.launch {
                                    load()
                                }
                            }
                        )
                    } else {
                        val value = project!!

                        Text(
                            text = stringResource(Res.string.home_hub_detail_project_title),
                            style = Theme[typography][h2].copy(color = Theme[colors][foreground])
                        )

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            val heroWidth = maxWidth
                            val wide = heroWidth >= 600.dp

                            val image: @Composable () -> Unit = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(20.dp))
                                ) {
                                    DesktopHubArtwork(
                                        url = value.thumbnailUrl?.let(repository.client::resolveUrl),
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        fallback = "▦",
                                    )

                                    value.youtubeUrl?.let { youtube ->
                                        val url = if (Regex("[A-Za-z0-9_-]{11}").matches(youtube)) {
                                            "https://www.youtube.com/watch?v=$youtube"
                                        } else {
                                            youtube
                                        }

                                        if (runCatching {
                                            java.net.URI.create(url).host?.lowercase() in setOf(
                                                "youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be", "www.youtu.be"
                                            )
                                        }.getOrDefault(false)) {
                                            Button(
                                                onClick = {
                                                    DesktopHubDownload.openExternal(url)
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.Center),
                                                variant = ButtonVariant.Default,
                                                size = ButtonSize.IconLarge,
                                                shape = CircleShape,
                                            ) {
                                                Icon(
                                                    imageVector = Lucide.Play,
                                                    contentDescription = stringResource(Res.string.home_hub_detail_watch_youtube),
                                                    modifier = Modifier
                                                        .size(26.dp),
                                                    tint = Theme[colors][primaryForeground],
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val details: @Composable () -> Unit = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    Text(
                                        text = value.title,
                                        style = Theme[typography][h2].copy(color = Theme[colors][foreground])
                                    )

                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                onNavigate(DesktopHubDestination.Artist(value.artist.username))
                                            },
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DesktopHubArtwork(
                                            url = value.artist.avatarUrl?.let(repository.client::resolveUrl),
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape),
                                            fallback = "◉",
                                        )

                                        Column {
                                            Text(
                                                text = value.artist.displayName.ifBlank { value.artist.username },
                                                style = Theme[typography][p].copy(color = Theme[colors][primary])
                                            )

                                            Text(
                                                text = "@${value.artist.username}",
                                                style = Theme[typography][p].copy(color = Theme[colors][mutedForeground])
                                            )
                                        }
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(9.dp)
                                    ) {
                                        DesktopHubStatus(
                                            icon = Lucide.FolderOpen,
                                            text = "${stringResource(Res.string.home_hub_detail_format)}: ${value.projectType.name.replaceFirstChar(Char::uppercase)}"
                                        )

                                        DesktopHubStatus(
                                            icon = Lucide.Check,
                                            text = "${stringResource(Res.string.home_hub_detail_compatibility)}: ${value.compatibility.name.replaceFirstChar(Char::uppercase)}"
                                        )

                                        DesktopHubDifficulty(
                                            label = stringResource(Res.string.home_hub_detail_difficulty),
                                            difficulty = value.difficulty
                                        )

                                        val timestamp = value.publishedAt ?: value.created
                                        if (timestamp > 0) {
                                            DesktopHubStatus(
                                                icon = Lucide.CalendarDays,
                                                text = "${stringResource(Res.string.home_hub_detail_published)} ${
                                                    DateTimeFormatter.ofPattern("d MMMM yyyy").format(
                                                        Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault())
                                                    )
                                                }"
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DesktopHubInlineMetric(
                                            icon = Lucide.Eye,
                                            count = value.views,
                                            label = stringResource(Res.string.home_hub_detail_views)
                                        )

                                        DesktopHubInlineMetric(
                                            icon = Lucide.Download,
                                            count = value.downloadsCount,
                                            label = stringResource(Res.string.home_hub_detail_downloads)
                                        )

                                        DesktopHubInlineMetric(
                                            icon = Lucide.Heart,
                                            count = likes,
                                            label = stringResource(Res.string.home_hub_detail_likes)
                                        )
                                    }

                                    val downloadUrl = DesktopHubDownload.downloadPageUrl(value, repository)

                                    BoxWithConstraints(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        val actions: @Composable () -> Unit = {
                                            if (
                                                downloadUrl != null && DesktopHubDownload.canImport(
                                                    project = value,
                                                    repository = repository,
                                                )
                                            ) {
                                                DesktopHubDownloadAction(
                                                    label = if (downloading) {
                                                        "${stringResource(Res.string.home_hub_detail_downloading)} ${(progress * 100).toInt()}%"
                                                    } else {
                                                        stringResource(Res.string.home_hub_detail_download_open)
                                                    },
                                                    downloading = downloading,
                                                    onClick = {
                                                        scope.launch {
                                                            downloading = true
                                                            progress = 0f
                                                            try {
                                                                val file = DesktopHubDownload.download(value, repository) {
                                                                    progress = it
                                                                }
                                                                onOpenDownloadedFile(file, value)
                                                            } catch (cancelled: CancellationException) {
                                                                throw cancelled
                                                            } catch (_: Exception) {
                                                                actionError = getString(Res.string.home_hub_detail_download_error)
                                                            } finally {
                                                                downloading = false
                                                            }
                                                        }
                                                    }
                                                )
                                            } else if (downloadUrl != null) {
                                                Button(
                                                    onClick = {
                                                        DesktopHubDownload.openExternal(downloadUrl)
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Lucide.ArrowUpRight,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(18.dp),
                                                        tint = Theme[colors][primaryForeground],
                                                    )

                                                    Text(
                                                        text = stringResource(Res.string.home_hub_detail_download)
                                                    )
                                                }
                                            }

                                            value.youtubeUrl?.let { youtube ->
                                                val url = if (Regex("[A-Za-z0-9_-]{11}").matches(youtube)) {
                                                    "https://www.youtube.com/watch?v=$youtube"
                                                } else {
                                                    youtube
                                                }

                                                if (runCatching {
                                                    java.net.URI.create(url).host?.lowercase() in setOf(
                                                        "youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be", "www.youtu.be"
                                                    )
                                                }.getOrDefault(false)) {
                                                    Button(
                                                        onClick = {
                                                            DesktopHubDownload.openExternal(url)
                                                        },
                                                        variant = ButtonVariant.Outline
                                                    ) {
                                                        Icon(
                                                            imageVector = Lucide.Play,
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(18.dp),
                                                            tint = Theme[colors][foreground],
                                                        )

                                                        Text(
                                                            text = stringResource(Res.string.home_hub_detail_watch_youtube)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (maxWidth < 450.dp) {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                actions()
                                            }
                                        } else {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                actions()
                                            }
                                        }
                                    }
                                }
                            }

                            if (wide) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width((heroWidth * 0.43f).coerceIn(260.dp, 460.dp))
                                    ) {
                                        image()
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                    ) {
                                        details()
                                    }
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(heroWidth.coerceAtMost(500.dp))
                                    ) {
                                        image()
                                    }

                                    details()
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Theme[colors][border])
                        )

                        val description = DesktopHubDownload.cleanDescription(value)
                        if (description.isNotBlank()) {
                            Text(
                                text = stringResource(Res.string.home_hub_detail_about),
                                style = Theme[typography][h3].copy(color = Theme[colors][foreground])
                            )

                            Text(
                                text = description,
                                style = Theme[typography][p].copy(color = Theme[colors][foreground])
                            )
                        }

                        Text(
                            text = stringResource(Res.string.home_hub_detail_details),
                            style = Theme[typography][h3].copy(color = Theme[colors][foreground])
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Theme[colors][card])
                                .border(
                                    width = 1.dp,
                                    color = Theme[colors][border],
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(13.dp)
                        ) {
                            value.overrideName?.let {
                                DesktopHubFact(
                                    label = stringResource(Res.string.home_hub_detail_amethyst_file),
                                    value = it
                                )
                            }

                            value.packageName?.let {
                                DesktopHubFact(
                                    label = stringResource(Res.string.home_hub_detail_package_file),
                                    value = it
                                )
                            }

                            value.packageSize?.let {
                                DesktopHubFact(
                                    label = stringResource(Res.string.home_hub_detail_size),
                                    value = "${it / 1024} KB"
                                )
                            }

                            value.packageSha256?.takeIf(String::isNotBlank)?.let {
                                DesktopHubFact(
                                    label = "SHA-256",
                                    value = it
                                )
                            }

                            DesktopHubDownload.externalUrl(value)?.let {
                                DesktopHubFact(
                                    label = stringResource(Res.string.home_hub_detail_download_source),
                                    value = runCatching { java.net.URI.create(it).host }.getOrNull() ?: it
                                )
                            }
                        }

                        if (actionError != null) {
                            DesktopHubRetry(
                                message = actionError!!,
                                retry = {
                                    actionError = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopHubLikedProjects(
    repository: HubRepository,
    sessionRevision: Any?,
    onNavigate: (DesktopHubDestination) -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    onProjects: () -> Unit = onBack,
) {
    val scope = rememberCoroutineScope()
    var projects by remember { mutableStateOf(emptyList<HubProject>()) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load(next: String?) {
        if (!repository.client.isAuthenticated) {
            projects = emptyList()
            cursor = null
            loading = false
            return
        }

        if (next == null) {
            loading = true
        } else {
            loadingMore = true
        }

        error = null

        try {
            val page = repository.getLikedProjects.execute(
                cursor = next,
                limit = 24
            )
            projects = if (next == null) {
                page.items
            } else {
                projects + page.items.filter { incoming ->
                    projects.none { it.id == incoming.id }
                }
            }
            cursor = page.nextCursor
        } catch (_: Exception) {
            error = getString(Res.string.home_hub_liked_error)
        } finally {
            loading = false
            loadingMore = false
        }
    }

    LaunchedEffect(repository, sessionRevision) {
        load(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (showBack) {
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp)
                } else {
                    Modifier
                }
            ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (showBack) {
            DesktopHubBack(
                onClick = onBack
            )
        }

        Text(
            text = stringResource(Res.string.home_hub_liked_title),
            style = Theme[typography][h2].copy(color = Theme[colors][foreground])
        )

        when {
            !repository.client.isAuthenticated -> {
                DesktopHubLikedEmpty(
                    title = stringResource(Res.string.home_hub_liked_sign_in),
                    description = stringResource(Res.string.home_hub_liked_empty_description),
                    action = stringResource(Res.string.home_hub_sign_in),
                    onAction = onSignIn,
                )
            }

            loading -> {
                DesktopHubMessage(
                    message = stringResource(Res.string.home_hub_detail_loading)
                )
            }

            error != null && projects.isEmpty() -> {
                DesktopHubRetry(
                    message = error!!,
                    retry = {
                        scope.launch {
                            load(null)
                        }
                    }
                )
            }

            projects.isEmpty() -> {
                DesktopHubLikedEmpty(
                    title = stringResource(Res.string.home_hub_liked_empty),
                    description = stringResource(Res.string.home_hub_liked_empty_description),
                    action = stringResource(Res.string.home_hub_search_projects),
                    onAction = onProjects,
                )
            }

            else -> {
                DesktopHubProjectGrid(
                    projects = projects,
                    repository = repository,
                    onOpenProject = {
                        onNavigate(DesktopHubDestination.Project(it.artist.username, it.slug))
                    },
                    onOpenArtist = {
                        onNavigate(DesktopHubDestination.Artist(it))
                    },
                    onSignIn = onSignIn,
                )

                if (cursor != null) {
                    DesktopHubButton(
                        label = if (loadingMore) {
                            stringResource(Res.string.home_hub_detail_loading)
                        } else {
                            stringResource(Res.string.home_hub_detail_load_more)
                        },
                        enabled = !loadingMore,
                        onClick = {
                            scope.launch {
                                load(cursor)
                            }
                        }
                    )
                }

                if (error != null) {
                    DesktopHubRetry(
                        message = error!!,
                        retry = {
                            scope.launch {
                                load(cursor)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopHubLikedEmpty(
    title: String,
    description: String,
    action: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Theme[colors][card]),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Theme[colors][primary].copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.Heart,
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp),
                tint = Theme[colors][primary]
            )
        }

        Spacer(
            modifier = Modifier
                .height(16.dp)
        )

        Text(
            text = title,
            modifier = Modifier
                .padding(horizontal = 20.dp),
            style = Theme[typography][h3].copy(color = Theme[colors][foreground]),
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier
                .height(6.dp)
        )

        Text(
            text = description,
            modifier = Modifier
                .padding(horizontal = 20.dp),
            style = Theme[typography][p].copy(color = Theme[colors][mutedForeground]),
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier
                .height(20.dp)
        )

        DesktopHubButton(
            label = action,
            onClick = onAction
        )
    }
}

@Composable
private fun DesktopHubProjectGrid(
    projects: List<HubProject>,
    repository: HubRepository,
    onOpenProject: (HubProject) -> Unit,
    onOpenArtist: (String) -> Unit,
    onSignIn: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val columns = when {
            maxWidth >= 900.dp -> 3
            maxWidth >= 600.dp -> 2
            else -> 1
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            projects.chunked(columns).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    row.forEach { project ->
                        DesktopHubProjectCard(
                            project = project,
                            repository = repository,
                            modifier = Modifier
                                .weight(1f),
                            onOpenArtist = onOpenArtist,
                            onOpenProject = { _, _ ->
                                onOpenProject(project)
                            },
                            onSignIn = onSignIn,
                        )
                    }

                    repeat(columns - row.size) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopHubFact(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier
                .width(150.dp),
            style = Theme[typography][p].copy(color = Theme[colors][mutedForeground])
        )

        Text(
            text = value,
            style = Theme[typography][p].copy(color = Theme[colors][foreground])
        )
    }
}

@Composable
private fun DesktopHubStatus(
    icon: ImageVector,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp),
            tint = Theme[colors][mutedForeground]
        )

        Text(
            text = text,
            style = Theme[typography][small].copy(color = Theme[colors][mutedForeground])
        )
    }
}

@Composable
private fun DesktopHubInlineMetric(
    icon: ImageVector,
    count: Long,
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(17.dp),
            tint = Theme[colors][mutedForeground]
        )

        Text(
            text = "$count $label",
            style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]),
            maxLines = 1
        )
    }
}

@Composable
private fun DesktopHubDifficulty(
    label: String,
    difficulty: Int,
) {
    val rating = difficulty.coerceIn(0, 10)

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                Icon(
                    imageVector = when {
                        rating >= (index + 1) * 2 -> Lucide.Star
                        rating == index * 2 + 1 -> Lucide.StarHalf
                        else -> Lucide.Star
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .size(17.dp),
                    tint = if (rating > index * 2) {
                        Color(0xFFFFB803)
                    } else {
                        Theme[colors][mutedForeground]
                    },
                )
            }

            Spacer(
                modifier = Modifier
                    .width(6.dp)
            )

            Text(
                text = "$label: ${rating / 2.0} / 5",
                style = Theme[typography][small].copy(color = Theme[colors][mutedForeground])
            )
        }
    }
}

@Composable
private fun DesktopHubMessage(
    message: String,
) {
    Text(
        text = message,
        style = Theme[typography][p].copy(color = Theme[colors][mutedForeground])
    )
}

@Composable
private fun DesktopHubRetry(
    message: String,
    retry: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DesktopHubMessage(
            message = message
        )

        DesktopHubButton(
            label = stringResource(Res.string.home_hub_retry),
            onClick = retry
        )
    }
}

@Composable
private fun DesktopHubBack(
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        variant = ButtonVariant.Ghost
    ) {
        Icon(
            imageVector = Lucide.ArrowLeft,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp),
            tint = Theme[colors][foreground]
        )

        Text(
            text = stringResource(Res.string.settings_back_desc)
        )
    }
}

@Composable
private fun DesktopHubButton(
    label: String,
    filled: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (filled) {
                    Theme[colors][primary]
                } else {
                    Theme[colors][card]
                }
            )
            .border(
                width = 1.dp,
                color = if (filled) {
                    Theme[colors][primary]
                } else {
                    Theme[colors][border]
                },
                shape = shape
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val foreground = if (filled) {
                Theme[colors][primaryForeground]
            } else {
                Theme[colors][foreground]
            }

            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp),
                    tint = foreground
                )
            }

            Text(
                text = label,
                style = Theme[typography][p].copy(
                    color = foreground,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun DesktopHubDownloadAction(
    label: String,
    downloading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp),
        enabled = !downloading
    ) {
        Icon(
            imageVector = Lucide.Download,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp),
            tint = Theme[colors][primaryForeground]
        )

        Text(
            text = label
        )
    }
}
