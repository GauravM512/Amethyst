package dev.anthonyhfm.amethyst.home.ui.views

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.composeunstyled.Icon
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import dev.anthonyhfm.amethyst.hub.data.*
import dev.anthonyhfm.amethyst.ui.components.primitives.Button
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonSize
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonVariant
import dev.anthonyhfm.amethyst.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image as SkiaImage
import java.net.URL

@Composable
internal fun DesktopHubFeed(
    repository: HubRepository,
    sessionRevision: Int,
    onOpenHref: (String?) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenProject: (String, String) -> Unit,
    onSignIn: () -> Unit,
) {
    var home by remember { mutableStateOf<HubHome?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(repository, sessionRevision, retry) {
        loading = true; error = false
        try { home = repository.getHome.execute() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = true }
        finally { loading = false }
    }
    when {
        home == null && loading -> DesktopHubMessage(stringResource(Res.string.home_hub_loading))
        home == null && error -> DesktopHubMessage(stringResource(Res.string.home_hub_error_generic)) { Button(onClick = { retry++ }) { Text(stringResource(Res.string.home_hub_retry)) } }
        else -> Column(verticalArrangement = Arrangement.spacedBy(48.dp)) {
            if (error) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.home_hub_error_generic), style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]))
                Button(onClick = { retry++ }, variant = ButtonVariant.Link) { Text(stringResource(Res.string.home_hub_retry)) }
            }
            if (home?.sections.isNullOrEmpty()) DesktopHubMessage(stringResource(Res.string.home_hub_empty_title))
            else home!!.sections.forEach { section -> DesktopHubSection(section, repository, onOpenHref, onOpenArtist, onOpenProject, onSignIn) }
            Button(onClick = { onOpenHref("/projects") }, variant = ButtonVariant.Outline) {
                Text(stringResource(Res.string.home_hub_show_all_projects))
            }
        }
    }
}

@Composable
private fun DesktopHubSection(
    section: HubHomeSection,
    repository: HubRepository,
    onOpenHref: (String?) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenProject: (String, String) -> Unit,
    onSignIn: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(section.title, modifier = Modifier.weight(1f), style = Theme[typography][h3].copy(color = Theme[colors][foreground]))
            if (!section.actionHref.isNullOrBlank() && !section.actionLabel.isNullOrBlank()) Button(onClick = { onOpenHref(section.actionHref) }, variant = ButtonVariant.Link) { Text(section.actionLabel!!) }
        }
        when (section) {
            is HubHomeSection.CreatorRow -> DesktopHubRow {
                section.items.forEach { artist ->
                    DesktopHubCreatorCard(artist, repository, onOpenArtist, onSignIn)
                }
            }
            is HubHomeSection.HeroCarousel -> DesktopHubRow {
                section.items.forEach { project ->
                    Column(Modifier.width(350.dp).clickable { onOpenHref(project.href) }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DesktopHubArtwork(project.imageUrl, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(20.dp)))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DesktopHubArtwork(project.creatorAvatarUrl, Modifier.size(38.dp).clip(CircleShape), "👤")
                            Column(Modifier.weight(1f)) {
                                Text(project.creatorName, style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]), maxLines = 1)
                                Text(project.title, style = Theme[typography][p].copy(color = Theme[colors][foreground]), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(onClick = { onOpenHref(project.href) }, variant = ButtonVariant.Secondary, size = ButtonSize.Icon, shape = CircleShape) {
                                Icon(Lucide.ArrowRight, contentDescription = project.title, modifier = Modifier.size(19.dp), tint = Theme[colors][foreground])
                            }
                        }
                    }
                }
            }
            is HubHomeSection.SquareCardRow -> DesktopHubSquareRow(section.items, onOpenHref)
            is HubHomeSection.MediaCardRow -> DesktopHubRow {
                section.items.forEach { project ->
                    Column(Modifier.width(350.dp).clickable { onOpenHref(project.href) }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DesktopHubArtwork(project.imageUrl, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(20.dp)))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(project.artist, style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]), maxLines = 1)
                                Text(project.title, style = Theme[typography][p].copy(color = Theme[colors][foreground]), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(onClick = { onOpenHref(project.href) }, variant = ButtonVariant.Secondary, size = ButtonSize.Icon, shape = CircleShape) {
                                Icon(Lucide.ArrowRight, contentDescription = project.title, modifier = Modifier.size(19.dp), tint = Theme[colors][foreground])
                            }
                        }
                    }
                }
            }
            is HubHomeSection.DetailedList -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                section.items.forEach { project ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onOpenHref(project.href) }.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        DesktopHubArtwork(project.imageUrl, Modifier.size(width = 150.dp, height = 85.dp))
                        Column(Modifier.weight(1f)) {
                            Text(project.title, style = Theme[typography][p].copy(color = Theme[colors][foreground]))
                            Text(project.description, style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]), maxLines = 2)
                            Text("${project.uploadedAt} · ${project.compatibility}", style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]))
                        }
                    }
                }
            }
            is HubHomeSection.CuratedSpotlight -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DesktopHubArtwork(section.creatorAvatarUrl, Modifier.size(48.dp).clip(CircleShape), "👤")
                    Column {
                        Text(section.creatorName, style = Theme[typography][p].copy(color = Theme[colors][foreground]))
                        Text(section.description, style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]))
                    }
                }
                DesktopHubSquareRow(section.items, onOpenHref)
            }
        }
    }
}

@Composable
private fun DesktopHubSquareRow(items: List<HubSquareCardItem>, onOpenHref: (String?) -> Unit) = DesktopHubRow {
    items.forEach { item ->
        Column(Modifier.width(148.dp).clickable { onOpenHref(item.href) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DesktopHubArtwork(item.imageUrl, Modifier.size(148.dp))
            Text(item.title, style = Theme[typography][p].copy(color = Theme[colors][foreground]), maxLines = 1)
            item.itemCount?.let { Text("$it ${stringResource(Res.string.home_hub_items)}", style = Theme[typography][small].copy(color = Theme[colors][mutedForeground])) }
        }
    }
}

@Composable
private fun DesktopHubCreatorCard(artist: HubCreatorItem, repository: HubRepository, onOpenArtist: (String) -> Unit, onSignIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var following by remember(artist.username, artist.isFollowing) { mutableStateOf(artist.isFollowing == true) }
    var followers by remember(artist.username, artist.followersCount) { mutableLongStateOf(artist.followersCount ?: 0L) }
    var busy by remember(artist.username) { mutableStateOf(false) }
    var failed by remember(artist.username) { mutableStateOf(false) }
    Column(Modifier.width(112.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(90.dp)) {
            DesktopHubArtwork(artist.imageUrl, Modifier.size(78.dp).align(Alignment.TopStart).clip(CircleShape).clickable { onOpenArtist(artist.username) }, "👤")
            Button(onClick = {
                if (!repository.client.isAuthenticated) { onSignIn(); return@Button }
                val previous = following; val previousCount = followers
                following = !following; followers = (followers + if (following) 1 else -1).coerceAtLeast(0)
                scope.launch {
                    busy = true; failed = false
                    try {
                        val result = if (following) repository.followArtist.execute(artist.username) else repository.unfollowArtist.execute(artist.username)
                        following = result.following; followers = result.followersCount
                    } catch (_: Exception) { following = previous; followers = previousCount; failed = true }
                    finally { busy = false }
                }
            }, modifier = Modifier.align(Alignment.BottomEnd), enabled = !busy,
                variant = if (following) ButtonVariant.Secondary else ButtonVariant.Default,
                size = ButtonSize.Icon, shape = CircleShape) {
                Icon(if (following) Lucide.Check else Lucide.Plus,
                    contentDescription = stringResource(if (following) Res.string.home_hub_unfollow else Res.string.home_hub_follow),
                    modifier = Modifier.size(17.dp), tint = Theme[colors][foreground])
            }
        }
        Text(artist.title, modifier = Modifier.clickable { onOpenArtist(artist.username) }, style = Theme[typography][p].copy(color = Theme[colors][foreground]), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("@${artist.username}", style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]), maxLines = 1)
        Text("$followers ${stringResource(if (followers == 1L) Res.string.home_hub_follower else Res.string.home_hub_followers)}", style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]))
        if (failed) Text(stringResource(Res.string.home_hub_follow_error), style = Theme[typography][small].copy(color = Theme[colors][destructive]))
    }
}

@Composable
internal fun DesktopHubRow(content: @Composable RowScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // The page text aligns at 32dp, while the carousel viewport reaches the outer edge.
        // Keep a trailing inset so the final card can still settle on the text alignment.
        Row(
            Modifier.width(maxWidth + 32.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            content()
            Spacer(Modifier.width(14.dp))
        }
    }
}

@Composable
internal fun DesktopHubMessage(message: String, action: @Composable (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 80.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(message, style = Theme[typography][p].copy(color = Theme[colors][mutedForeground]))
        action?.invoke()
    }
}

@Composable
internal fun DesktopHubArtwork(url: String?, modifier: Modifier, fallback: String = "♪") {
    var image by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        image = null
        if (!url.isNullOrBlank()) {
            image = try { withContext(Dispatchers.IO) {
                val resolved = when {
                    url.startsWith("http://") || url.startsWith("https://") -> url
                    url.startsWith("/api/") -> HubApiClient.DEFAULT_BASE_URL + url.removePrefix("/api")
                    else -> HubApiClient.DEFAULT_BASE_URL + "/" + url.trimStart('/')
                }
                SkiaImage.makeFromEncoded(URL(resolved).openStream().use { it.readBytes() }).toComposeImageBitmap()
            } }
            catch (_: Exception) { null }
        }
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Theme[colors][card]).border(1.dp, Theme[colors][border], RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        if (image != null) Image(image!!, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(fallback, style = Theme[typography][h3].copy(color = Theme[colors][mutedForeground]))
    }
}
