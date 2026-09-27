package dev.anthonyhfm.amethyst.home.ui.views

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.Icon
import com.composeunstyled.theme.Theme
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import dev.anthonyhfm.amethyst.hub.data.HubArtist
import dev.anthonyhfm.amethyst.hub.data.HubProject
import dev.anthonyhfm.amethyst.hub.data.HubRepository
import dev.anthonyhfm.amethyst.settings.data.HubSettings
import dev.anthonyhfm.amethyst.ui.theme.border
import dev.anthonyhfm.amethyst.ui.theme.card
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.foreground
import dev.anthonyhfm.amethyst.ui.theme.h2
import dev.anthonyhfm.amethyst.ui.theme.h3
import dev.anthonyhfm.amethyst.ui.theme.h4
import dev.anthonyhfm.amethyst.ui.theme.mutedForeground
import dev.anthonyhfm.amethyst.ui.theme.p
import dev.anthonyhfm.amethyst.ui.theme.primary
import dev.anthonyhfm.amethyst.ui.theme.primaryForeground
import dev.anthonyhfm.amethyst.ui.theme.typography
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
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
        is DesktopHubDestination.Artist -> DesktopHubArtistDetail(destination.username, repository, onBack, onNavigate, onSignIn, modifier)
        is DesktopHubDestination.Project -> DesktopHubProjectDetail(destination.username, destination.slug, repository, onBack, onNavigate, onSignIn, onOpenDownloadedFile, modifier)
    }
}

@Composable
private fun DesktopHubArtistDetail(
    username: String, repository: HubRepository, onBack: () -> Unit,
    onNavigate: (DesktopHubDestination) -> Unit, onSignIn: () -> Unit, modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    var artist by remember(username) { mutableStateOf<HubArtist?>(null) }
    var projects by remember(username) { mutableStateOf(emptyList<HubProject>()) }
    var cursor by remember(username) { mutableStateOf<String?>(null) }
    var loading by remember(username) { mutableStateOf(true) }
    var loadingMore by remember(username) { mutableStateOf(false) }
    var following by remember(username) { mutableStateOf(false) }
    var followers by remember(username) { mutableStateOf(0L) }
    var followPending by remember(username) { mutableStateOf(false) }
    var error by remember(username) { mutableStateOf<String?>(null) }
    var actionError by remember(username) { mutableStateOf<String?>(null) }

    suspend fun loadProjects(next: String?) {
        if (loadingMore) return
        loadingMore = true
        try {
            val page = repository.getArtistProjects.execute(username, next, 24)
            projects = if (next == null) page.items else projects + page.items.filter { incoming -> projects.none { it.id == incoming.id } }
            cursor = page.nextCursor
        } catch (_: Exception) { actionError = getString(Res.string.home_hub_detail_projects_error) }
        loadingMore = false
    }
    suspend fun load() {
        loading = true; error = null
        try {
            artist = repository.getArtist.execute(username)
            following = artist!!.isFollowing
            followers = artist!!.followersCount
            projects = emptyList(); cursor = null
            loadProjects(null)
        } catch (_: Exception) { error = getString(Res.string.home_hub_detail_artist_error) }
        loading = false
    }
    LaunchedEffect(username, repository) { load() }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val sidePadding = if (maxWidth < 600.dp) 24.dp else 32.dp
        val contentWidth = (maxWidth - sidePadding * 2).coerceAtMost(1200.dp)
        Column(Modifier.fillMaxSize().clipToBounds().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.width(contentWidth).align(Alignment.CenterHorizontally).padding(top = 28.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                DesktopHubLink("← ${stringResource(Res.string.settings_back_desc)}", onBack)
                if (loading && artist == null) DesktopHubMessage(stringResource(Res.string.home_hub_detail_loading))
                else if (artist == null) DesktopHubRetry(error ?: stringResource(Res.string.home_hub_detail_artist_unavailable)) { scope.launch { load() } }
                else {
                    val value = artist!!
                    Text(stringResource(Res.string.home_hub_detail_artist_title), style = Theme[typography][h2].copy(color = Theme[colors][foreground]))
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val wide = maxWidth >= 680.dp
                        val avatarSize = if (wide) 144.dp else 84.dp
                        val followButton: @Composable () -> Unit = {
                            DesktopHubButton(if (following) "✓ ${stringResource(Res.string.home_hub_following)}" else "+ ${stringResource(Res.string.home_hub_follow)}", enabled = !followPending) {
                                if (!repository.client.isAuthenticated) { onSignIn(); return@DesktopHubButton }
                                scope.launch {
                                    followPending = true
                                    try {
                                        val result = if (following) repository.unfollowArtist.execute(username) else repository.followArtist.execute(username)
                                        following = result.following; followers = result.followersCount
                                    } catch (_: Exception) { actionError = getString(Res.string.home_hub_follow_error) }
                                    followPending = false
                                }
                            }
                        }
                        val shareButton: @Composable () -> Unit = {
                            DesktopHubButton(stringResource(Res.string.home_hub_detail_share), filled = false) { DesktopHubDownload.openExternal("https://projects.launchpadders.com/@$username") }
                        }
                        val identity: @Composable () -> Unit = {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(value.displayName.ifBlank { value.username }, style = Theme[typography][if (wide) h2 else h3].copy(color = Theme[colors][foreground]), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("@${value.username}", style = Theme[typography][p].copy(color = Theme[colors][mutedForeground]), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${value.publishedProjectCount} ${stringResource(Res.string.home_hub_detail_projects)}  ·  $followers ${stringResource(Res.string.home_hub_followers)}", style = Theme[typography][p].copy(color = Theme[colors][mutedForeground]))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (wide) 32.dp else 18.dp), verticalAlignment = Alignment.Top) {
                            DesktopHubArtwork(value.avatarUrl?.let(repository.client::resolveUrl), Modifier.size(avatarSize).clip(CircleShape), "◉")
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                identity()
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    followButton()
                                    shareButton()
                                }
                            }
                        }
                    }
                    if (value.bio.isNotBlank()) Text(value.bio, style = Theme[typography][p].copy(color = Theme[colors][foreground]))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Theme[colors][border]))
                    Text(stringResource(Res.string.home_hub_detail_projects_title), style = Theme[typography][h3].copy(color = Theme[colors][foreground]))
                    if (projects.isEmpty()) DesktopHubMessage(stringResource(Res.string.home_hub_detail_no_projects))
                    else DesktopHubProjectGrid(
                        projects = projects,
                        repository = repository,
                        onOpenProject = { onNavigate(DesktopHubDestination.Project(username, it.slug)) },
                        onOpenArtist = { onNavigate(DesktopHubDestination.Artist(it)) },
                        onSignIn = onSignIn,
                    )
                    if (cursor != null) DesktopHubButton(if (loadingMore) stringResource(Res.string.home_hub_detail_loading) else stringResource(Res.string.home_hub_detail_load_more), enabled = !loadingMore) {
                        scope.launch { loadProjects(cursor) }
                    }
                    if (actionError != null) DesktopHubRetry(actionError!!) { actionError = null }
                }
            }
        }
    }
}

@Composable
private fun DesktopHubProjectDetail(
    username: String, slug: String, repository: HubRepository, onBack: () -> Unit,
    onNavigate: (DesktopHubDestination) -> Unit, onSignIn: () -> Unit,
    onOpenDownloadedFile: suspend (File, HubProject) -> Unit, modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    var project by remember(username, slug) { mutableStateOf<HubProject?>(null) }
    var loading by remember(username, slug) { mutableStateOf(true) }
    var error by remember(username, slug) { mutableStateOf<String?>(null) }
    var actionError by remember(username, slug) { mutableStateOf<String?>(null) }
    var liked by remember(username, slug) { mutableStateOf(false) }
    var likes by remember(username, slug) { mutableStateOf(0L) }
    var likePending by remember(username, slug) { mutableStateOf(false) }
    var downloading by remember(username, slug) { mutableStateOf(false) }
    var progress by remember(username, slug) { mutableStateOf(0f) }

    suspend fun load() {
        loading = true; error = null
        try {
            project = repository.getPublishedProject.execute(username, slug)
            liked = project!!.isLiked; likes = project!!.likesCount
        } catch (_: Exception) { error = getString(Res.string.home_hub_detail_project_error) }
        loading = false
    }
    LaunchedEffect(username, slug, repository) { load() }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val sidePadding = if (maxWidth < 600.dp) 24.dp else 32.dp
        val contentWidth = (maxWidth - sidePadding * 2).coerceAtMost(1140.dp)
        Column(Modifier.fillMaxSize().clipToBounds().verticalScroll(rememberScrollState())) {
          Column(
            Modifier.width(contentWidth).align(Alignment.CenterHorizontally).padding(top = 28.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
          ) {
        DesktopHubLink("← ${stringResource(Res.string.settings_back_desc)}", onBack)
        if (loading && project == null) DesktopHubMessage(stringResource(Res.string.home_hub_detail_loading))
        else if (project == null) DesktopHubRetry(error ?: stringResource(Res.string.home_hub_detail_project_unavailable)) { scope.launch { load() } }
        else {
            val value = project!!
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val heroWidth = maxWidth
                val wide = heroWidth >= 820.dp
                val image: @Composable () -> Unit = {
                    DesktopHubArtwork(value.thumbnailUrl?.let(repository.client::resolveUrl), Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(24.dp)), "▦")
                }
                val details: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text(value.title, style = Theme[typography][h2].copy(color = Theme[colors][foreground]))
                        Row(Modifier.clickable { onNavigate(DesktopHubDestination.Artist(value.artist.username)) }, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            DesktopHubArtwork(value.artist.avatarUrl?.let(repository.client::resolveUrl), Modifier.size(38.dp).clip(CircleShape), "◉")
                            Column {
                                Text(value.artist.displayName.ifBlank { value.artist.username }, style = Theme[typography][p].copy(color = Theme[colors][primary]))
                                Text("@${value.artist.username}", style = Theme[typography][p].copy(color = Theme[colors][mutedForeground]))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            DesktopHubMetric("${value.views}", stringResource(Res.string.home_hub_detail_views))
                            DesktopHubMetric("${value.downloadsCount}", stringResource(Res.string.home_hub_detail_downloads))
                            DesktopHubMetric("$likes", stringResource(Res.string.home_hub_detail_likes))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DesktopHubButton(if (liked) "♥ ${stringResource(Res.string.home_hub_detail_unlike)}" else "♡ ${stringResource(Res.string.home_hub_detail_like)}", filled = false, enabled = !likePending) {
                                if (!repository.client.isAuthenticated) { onSignIn(); return@DesktopHubButton }
                                scope.launch {
                                    likePending = true
                                    try { repository.toggleProjectLike.execute(value.id).also { liked = it.liked; likes = it.likesCount } }
                                    catch (_: Exception) { actionError = getString(Res.string.home_hub_detail_like_error) }
                                    likePending = false
                                }
                            }
                            DesktopHubButton(stringResource(Res.string.home_hub_detail_share), filled = false) { DesktopHubDownload.openExternal("https://projects.launchpadders.com/@$username/$slug") }
                        }
                        val downloadUrl = DesktopHubDownload.downloadPageUrl(value, repository)
                        val allowed = HubSettings.ignoreCompatibility.value || value.projectType.name == "amethyst" ||
                            value.compatibility.name == "compatible" || value.overrideDownloadUrl != null
                        if (downloadUrl != null && allowed && DesktopHubDownload.canImport(value, repository)) {
                            DesktopHubDownloadAction(
                                label = if (downloading) "↓ ${stringResource(Res.string.home_hub_detail_downloading)} ${(progress * 100).toInt()}%" else "↓ ${stringResource(Res.string.home_hub_detail_download_open)}",
                                downloading = downloading,
                                progress = progress,
                            ) {
                                scope.launch {
                                    downloading = true; progress = 0f
                                    try {
                                        val file = DesktopHubDownload.download(value, repository) { progress = it }
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
                        } else if (downloadUrl != null) DesktopHubButton("↗ ${stringResource(Res.string.home_hub_detail_download)}", filled = true) { DesktopHubDownload.openExternal(downloadUrl) }
                        value.youtubeUrl?.let { youtube ->
                            val url = if (Regex("[A-Za-z0-9_-]{11}").matches(youtube)) "https://www.youtube.com/watch?v=$youtube" else youtube
                            if (runCatching { java.net.URI.create(url).host?.lowercase() in setOf("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be", "www.youtu.be") }.getOrDefault(false))
                                DesktopHubButton("▶ ${stringResource(Res.string.home_hub_detail_watch_youtube)}", filled = false) { DesktopHubDownload.openExternal(url) }
                        }
                    }
                }
                if (wide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Box(Modifier.width(if (heroWidth < 1024.dp) 380.dp else 460.dp)) { image() }
                    Box(Modifier.weight(1f)) { details() }
                } else Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.width(heroWidth.coerceAtMost(500.dp))) { image() }
                    details()
                }
            }
            val description = DesktopHubDownload.cleanDescription(value)
            if (description.isNotBlank()) {
                Text(stringResource(Res.string.home_hub_detail_about), style = Theme[typography][h3].copy(color = Theme[colors][foreground]))
                Text(description, style = Theme[typography][p].copy(color = Theme[colors][foreground]))
            }
            Text(stringResource(Res.string.home_hub_detail_details), style = Theme[typography][h3].copy(color = Theme[colors][foreground]))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Theme[colors][card]).border(1.dp, Theme[colors][border], RoundedCornerShape(18.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                DesktopHubFact(stringResource(Res.string.home_hub_detail_format), value.projectType.name.replaceFirstChar(Char::uppercase))
                DesktopHubFact(stringResource(Res.string.home_hub_detail_compatibility), value.compatibility.name.replaceFirstChar(Char::uppercase))
                DesktopHubFact(stringResource(Res.string.home_hub_detail_difficulty), "${value.difficulty.coerceIn(0, 10) / 2.0} / 5")
                val timestamp = value.publishedAt ?: value.created
                if (timestamp > 0) DesktopHubFact(stringResource(Res.string.home_hub_detail_published), DateTimeFormatter.ofPattern("d MMMM yyyy").format(Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault())))
                value.overrideName?.let { DesktopHubFact(stringResource(Res.string.home_hub_detail_amethyst_file), it) }
                value.packageName?.let { DesktopHubFact(stringResource(Res.string.home_hub_detail_package_file), it) }
                value.packageSize?.let { DesktopHubFact(stringResource(Res.string.home_hub_detail_size), "${it / 1024} KB") }
                value.packageSha256?.takeIf(String::isNotBlank)?.let { DesktopHubFact("SHA-256", it) }
                DesktopHubDownload.externalUrl(value)?.let { DesktopHubFact(stringResource(Res.string.home_hub_detail_download_source), runCatching { java.net.URI.create(it).host }.getOrNull() ?: it) }
            }
            if (actionError != null) DesktopHubRetry(actionError!!) { actionError = null }
          }
        }
    }
}
}

@Composable
fun DesktopHubLikedProjects(
    repository: HubRepository, sessionRevision: Any?, onNavigate: (DesktopHubDestination) -> Unit,
    onSignIn: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier,
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
        if (!repository.client.isAuthenticated) { projects = emptyList(); cursor = null; loading = false; return }
        if (next == null) loading = true else loadingMore = true
        error = null
        try {
            val page = repository.getLikedProjects.execute(next, 24)
            projects = if (next == null) page.items else projects + page.items.filter { incoming -> projects.none { it.id == incoming.id } }
            cursor = page.nextCursor
        } catch (_: Exception) { error = getString(Res.string.home_hub_liked_error) }
        loading = false; loadingMore = false
    }
    LaunchedEffect(repository, sessionRevision) { load(null) }
    Column(modifier.fillMaxSize().then(if (showBack) Modifier.verticalScroll(rememberScrollState()).padding(32.dp) else Modifier), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (showBack) DesktopHubLink("← ${stringResource(Res.string.settings_back_desc)}", onBack)
        Text(stringResource(Res.string.home_hub_liked_title), style = Theme[typography][h2].copy(color = Theme[colors][foreground]))
        when {
            !repository.client.isAuthenticated -> DesktopHubLikedEmpty(
                title = stringResource(Res.string.home_hub_liked_sign_in),
                description = stringResource(Res.string.home_hub_liked_empty_description),
                action = stringResource(Res.string.home_hub_sign_in),
                onAction = onSignIn,
            )
            loading -> DesktopHubMessage(stringResource(Res.string.home_hub_detail_loading))
            error != null && projects.isEmpty() -> DesktopHubRetry(error!!) { scope.launch { load(null) } }
            projects.isEmpty() -> DesktopHubLikedEmpty(
                title = stringResource(Res.string.home_hub_liked_empty),
                description = stringResource(Res.string.home_hub_liked_empty_description),
                action = stringResource(Res.string.home_hub_search_projects),
                onAction = onProjects,
            )
            else -> {
                DesktopHubProjectGrid(
                    projects = projects,
                    repository = repository,
                    onOpenProject = { onNavigate(DesktopHubDestination.Project(it.artist.username, it.slug)) },
                    onOpenArtist = { onNavigate(DesktopHubDestination.Artist(it)) },
                    onSignIn = onSignIn,
                )
                if (cursor != null) DesktopHubButton(if (loadingMore) stringResource(Res.string.home_hub_detail_loading) else stringResource(Res.string.home_hub_detail_load_more), enabled = !loadingMore) { scope.launch { load(cursor) } }
                if (error != null) DesktopHubRetry(error!!) { scope.launch { load(cursor) } }
            }
        }
    }
}

@Composable
private fun DesktopHubLikedEmpty(title: String, description: String, action: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)).background(Theme[colors][card]),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Theme[colors][primary].copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(Lucide.Heart, contentDescription = null, modifier = Modifier.size(30.dp), tint = Theme[colors][primary])
        }
        Spacer(Modifier.height(16.dp))
        Text(title, modifier = Modifier.padding(horizontal = 20.dp), style = Theme[typography][h3].copy(color = Theme[colors][foreground]), textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(description, modifier = Modifier.padding(horizontal = 20.dp), style = Theme[typography][p].copy(color = Theme[colors][mutedForeground]), textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        DesktopHubButton(action, onClick = onAction)
    }
}

@Composable
private fun DesktopHubProjectGrid(
    projects: List<HubProject>, repository: HubRepository,
    onOpenProject: (HubProject) -> Unit, onOpenArtist: (String) -> Unit, onSignIn: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 900.dp) 3 else if (maxWidth >= 600.dp) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            projects.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    row.forEach { project ->
                        DesktopHubProjectCard(
                            project = project,
                            repository = repository,
                            modifier = Modifier.weight(1f),
                            onOpenArtist = onOpenArtist,
                            onOpenProject = { _, _ -> onOpenProject(project) },
                            onSignIn = onSignIn,
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable private fun DesktopHubMetric(value: String, label: String) {
    Column {
        Text(value, style = Theme[typography][h4].copy(color = Theme[colors][foreground]))
        Text(label, style = Theme[typography][p].copy(color = Theme[colors][mutedForeground]))
    }
}

@Composable private fun DesktopHubFact(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, modifier = Modifier.width(150.dp), style = Theme[typography][p].copy(color = Theme[colors][mutedForeground]))
        Text(value, style = Theme[typography][p].copy(color = Theme[colors][foreground]))
    }
}

@Composable private fun DesktopHubMessage(message: String) {
    Text(message, style = Theme[typography][p].copy(color = Theme[colors][mutedForeground]))
}

@Composable private fun DesktopHubRetry(message: String, retry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DesktopHubMessage(message)
        DesktopHubButton(stringResource(Res.string.home_hub_retry), onClick = retry)
    }
}

@Composable private fun DesktopHubLink(label: String, onClick: () -> Unit) {
    Text(label, modifier = Modifier.clickable(onClick = onClick), style = Theme[typography][p].copy(color = Theme[colors][foreground]))
}

@Composable private fun DesktopHubButton(label: String, filled: Boolean = true, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.clip(shape).background(if (filled) Theme[colors][primary] else Theme[colors][card])
        .border(1.dp, if (filled) Theme[colors][primary] else Theme[colors][border], shape)
        .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
        Text(label, style = Theme[typography][p].copy(color = if (filled) Theme[colors][primaryForeground] else Theme[colors][foreground], fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun DesktopHubDownloadAction(label: String, downloading: Boolean, progress: Float, onClick: () -> Unit) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 200),
        label = "Hub download progress",
    )
    val shape = CircleShape
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(52.dp).clip(shape)
            .background(Theme[colors][primary].copy(alpha = if (downloading) 0.7f else 1f))
            .clickable(enabled = !downloading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (downloading && animatedProgress > 0f) {
            Box(
                Modifier.width(maxWidth * animatedProgress).fillMaxHeight()
                    .align(Alignment.CenterStart).background(Theme[colors][primary]),
            )
        }
        Text(
            label,
            style = Theme[typography][p].copy(
                color = Theme[colors][primaryForeground],
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}
