package dev.anthonyhfm.amethyst.home.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import dev.anthonyhfm.amethyst.hub.data.HubCreatorItem
import dev.anthonyhfm.amethyst.hub.data.HubHomeSection
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.text.NumberFormat

private val hubEdge = 20.dp

@Composable
internal fun HubHomeSectionView(
    section: HubHomeSection,
    account: AndroidHubAccount,
    onOpenHref: (String?) -> Unit,
    onSignIn: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HubSectionHeader(section.title, section.actionLabel, section.actionHref, onOpenHref)
        when (section) {
            is HubHomeSection.CreatorRow -> LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = hubEdge),
            ) {
                items(section.items, key = { it.id }) { artist ->
                    HubCreatorCard(artist, account, onOpenHref, onSignIn)
                }
            }
            is HubHomeSection.HeroCarousel -> BoxWithConstraints {
                val cardWidth = (maxWidth - 56.dp).coerceIn(260.dp, 420.dp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = hubEdge),
                ) {
                    items(section.items, key = { it.id }) { project ->
                        Column(Modifier.width(cardWidth).clickable { onOpenHref(project.href) }) {
                            HubArtwork(project.imageUrl, Modifier.fillMaxWidth().aspectRatio(1.55f), MaterialTheme.shapes.extraLarge)
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                HubArtwork(project.creatorAvatarUrl, Modifier.size(38.dp), CircleShape, Icons.Default.Person)
                                Column(Modifier.weight(1f)) {
                                    Text(project.creatorName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
            is HubHomeSection.SquareCardRow -> SquareHubRow(section.items, onOpenHref)
            is HubHomeSection.MediaCardRow -> LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = hubEdge),
            ) {
                items(section.items, key = { it.id }) { item ->
                    Column(Modifier.width(156.dp).clickable { onOpenHref(item.href) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        HubArtwork(item.imageUrl, Modifier.size(156.dp), MaterialTheme.shapes.large)
                        Text(item.artist, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(item.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            is HubHomeSection.DetailedList -> Column(Modifier.padding(horizontal = hubEdge), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                section.items.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenHref(item.href) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        HubArtwork(item.imageUrl, Modifier.size(88.dp), MaterialTheme.shapes.large)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${item.uploadedAt} · ${item.compatibility}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            is HubHomeSection.CuratedSpotlight -> {
                Row(Modifier.fillMaxWidth().padding(horizontal = hubEdge), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HubArtwork(section.creatorAvatarUrl, Modifier.size(42.dp), CircleShape, Icons.Default.Person)
                    Column {
                        Text(section.creatorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(section.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                SquareHubRow(section.items, onOpenHref)
            }
        }
    }
}

@Composable
private fun HubCreatorCard(
    artist: HubCreatorItem,
    account: AndroidHubAccount,
    onOpenHref: (String?) -> Unit,
    onSignIn: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var following by remember(artist.username, account.sessionRevision) { mutableStateOf(artist.isFollowing == true) }
    var followers by remember(artist.username, account.sessionRevision) { mutableStateOf(artist.followersCount ?: 0L) }
    var busy by remember(artist.username) { mutableStateOf(false) }
    var failed by remember(artist.username) { mutableStateOf(false) }
    val followLabel = stringResource(if (following) Res.string.home_hub_unfollow else Res.string.home_hub_follow)
    if (failed) AlertDialog(
        onDismissRequest = { failed = false },
        title = { Text(stringResource(Res.string.home_hub_title)) },
        text = { Text(stringResource(Res.string.home_hub_follow_error)) },
        confirmButton = { TextButton(onClick = { failed = false }) { Text(stringResource(Res.string.home_hub_dismiss)) } },
    )
    Box(Modifier.width(92.dp)) {
        Column(
            Modifier.width(92.dp).clickable { onOpenHref(artist.href ?: "/@${artist.username}") },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.TopCenter) {
                HubArtwork(artist.imageUrl, Modifier.size(76.dp), CircleShape, Icons.Default.Person)
            }
            Text(artist.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@${artist.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${NumberFormat.getIntegerInstance().format(followers)} ${stringResource(if (followers == 1L) Res.string.home_hub_follower else Res.string.home_hub_followers)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(
            onClick = {
                if (!account.repository.client.isAuthenticated) { onSignIn(); return@IconButton }
                val oldFollowing = following
                val oldFollowers = followers
                following = !following
                followers = (followers + if (following) 1 else -1).coerceAtLeast(0)
                scope.launch {
                    busy = true
                    try {
                        val result = if (following) account.repository.followArtist.execute(artist.username)
                            else account.repository.unfollowArtist.execute(artist.username)
                        following = result.following
                        followers = result.followersCount
                    } catch (_: Exception) {
                        following = oldFollowing
                        followers = oldFollowers
                        failed = true
                    } finally { busy = false }
                }
            },
            enabled = !busy,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp).size(48.dp),
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = if (following) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.background),
            ) {
                if (busy) CircularProgressIndicator(Modifier.padding(5.dp).size(14.dp), strokeWidth = 2.dp)
                else Icon(
                    if (following) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = "$followLabel ${artist.title}",
                    modifier = Modifier.padding(5.dp).size(14.dp),
                    tint = if (following) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun HubSectionHeader(title: String, actionLabel: String?, actionHref: String?, onOpenHref: (String?) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = hubEdge), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (!actionLabel.isNullOrBlank() && !actionHref.isNullOrBlank()) {
            TextButton(onClick = { onOpenHref(actionHref) }) { Text(actionLabel) }
        }
    }
}

@Composable
private fun SquareHubRow(items: List<dev.anthonyhfm.amethyst.hub.data.HubSquareCardItem>, onOpenHref: (String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = hubEdge),
    ) {
        items(items, key = { it.id }) { item ->
            Column(Modifier.width(108.dp).clickable { onOpenHref(item.href) }, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                HubArtwork(item.imageUrl, Modifier.size(108.dp), MaterialTheme.shapes.large, Icons.Default.QueueMusic)
                Text(item.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                item.itemCount?.let { Text("$it items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
