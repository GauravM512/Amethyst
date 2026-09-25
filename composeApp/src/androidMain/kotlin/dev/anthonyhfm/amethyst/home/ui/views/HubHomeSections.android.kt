package dev.anthonyhfm.amethyst.home.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.hub.data.HubHomeSection

private val hubEdge = 20.dp

@Composable
internal fun HubHomeSectionView(
    section: HubHomeSection,
    onOpenHref: (String?) -> Unit,
    onAction: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HubSectionHeader(section.title, section.actionHref) { onAction(section.actionHref) }
        when (section) {
            is HubHomeSection.CreatorRow -> LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = hubEdge),
            ) {
                items(section.items, key = { it.id }) { artist ->
                    Column(
                        Modifier.width(104.dp).clickable { onOpenHref(artist.href ?: "/@${artist.username}") },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        HubArtwork(artist.imageUrl, Modifier.size(88.dp), CircleShape, Icons.Default.Person)
                        Text(artist.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("@${artist.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            is HubHomeSection.HeroCarousel -> BoxWithConstraints {
                val cardWidth = (maxWidth - 56.dp).coerceIn(260.dp, 420.dp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = hubEdge),
                ) {
                    items(section.items, key = { it.id }) { project ->
                        Card(
                            onClick = { onOpenHref(project.href) },
                            modifier = Modifier.width(cardWidth),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        ) {
                            HubArtwork(project.imageUrl, Modifier.fillMaxWidth().aspectRatio(1.55f), MaterialTheme.shapes.extraLarge)
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                HubArtwork(project.creatorAvatarUrl, Modifier.size(38.dp), CircleShape, Icons.Default.Person)
                                Column(Modifier.weight(1f)) {
                                    Text(project.creatorName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
private fun HubSectionHeader(title: String, actionHref: String?, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = hubEdge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (actionHref != null) Surface(onClick = onAction, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = title, modifier = Modifier.padding(8.dp))
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
