package dev.anthonyhfm.amethyst.home.ui.views

import amethyst.composeapp.generated.resources.*
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music2
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.UserRound
import com.composeunstyled.Icon
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.home.data.DownloadedProjectDetails
import dev.anthonyhfm.amethyst.hub.data.rememberHubImage
import dev.anthonyhfm.amethyst.ui.components.primitives.Button
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonSize
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonVariant
import dev.anthonyhfm.amethyst.ui.components.primitives.DropdownMenu
import dev.anthonyhfm.amethyst.ui.components.primitives.DropdownMenuContent
import dev.anthonyhfm.amethyst.ui.components.primitives.DropdownMenuItem
import dev.anthonyhfm.amethyst.ui.components.primitives.DropdownMenuSeparator
import dev.anthonyhfm.amethyst.ui.components.primitives.DropdownMenuTrigger
import dev.anthonyhfm.amethyst.ui.modifier.rightClickable
import dev.anthonyhfm.amethyst.ui.theme.*
import dev.anthonyhfm.amethyst.workspace.data.RecentWorkspace
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DownloadedProjectsGrid(
    projects: List<RecentWorkspace>,
    details: Map<String, DownloadedProjectDetails>,
    onOpen: (RecentWorkspace) -> Unit,
    onNavigate: (DesktopHubDestination) -> Unit,
    onRemove: (RecentWorkspace) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        val columnCount = ((maxWidth + 16.dp) / 276.dp).toInt().coerceIn(minimumValue = 1, maximumValue = 4)

        Column(
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            projects.chunked(size = columnCount).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                ) {
                    row.forEach { project ->
                        key(project.path) {
                            DownloadedProjectCard(
                                project = project,
                                details = details[project.path],
                                onOpen = { onOpen(project) },
                                onNavigate = onNavigate,
                                onRemove = { onRemove(project) },
                                modifier = Modifier
                                    .weight(weight = 1f),
                            )
                        }
                    }

                    repeat(times = columnCount - row.size) {
                        Spacer(
                            modifier = Modifier
                                .weight(weight = 1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadedProjectCard(
    project: RecentWorkspace,
    details: DownloadedProjectDetails?,
    onOpen: () -> Unit,
    onNavigate: (DesktopHubDestination) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var actionsExpanded by remember(key1 = project.path) { mutableStateOf(value = false) }
    val shape = RoundedCornerShape(size = 16.dp)
    val openLabel = stringResource(resource = Res.string.home_projects_item_menu_open)

    Column(
        modifier = modifier
            .clip(shape = shape)
            .border(width = 1.dp, color = Theme[colors][border], shape = shape)
            .background(color = Theme[colors][card])
            .rightClickable(onRightClick = { actionsExpanded = true }),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen),
        ) {
            DownloadedProjectArtwork(
                url = details?.thumbnailUrl,
                fallback = Lucide.Music2,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio = 16f / 9f),
            )

            Text(
                text = project.title,
                style = Theme[typography][p],
                fontWeight = FontWeight.SemiBold,
                color = Theme[colors][cardForeground],
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            DownloadedProjectArtwork(
                url = details?.artist?.avatarUrl,
                fallback = Lucide.UserRound,
                fallbackSize = 16.dp,
                modifier = Modifier
                    .size(size = 24.dp)
                    .clip(shape = CircleShape),
            )

            Text(
                text = details?.artist?.let { it.displayName.ifBlank { "@${it.username}" } }
                    ?: stringResource(resource = Res.string.home_projects_recent_downloaded),
                style = Theme[typography][small],
                color = Theme[colors][mutedForeground],
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            Button(
                onClick = onOpen,
                variant = ButtonVariant.Outline,
                size = ButtonSize.Small,
                modifier = Modifier
                    .weight(weight = 1f),
            ) {
                Icon(
                    imageVector = Lucide.FolderOpen,
                    contentDescription = null,
                    tint = Theme[colors][foreground],
                    modifier = Modifier
                        .size(size = 16.dp),
                )

                Text(text = openLabel)
            }

            DropdownMenu(
                expanded = actionsExpanded,
                onExpandRequest = { actionsExpanded = true },
                onDismissRequest = { actionsExpanded = false },
            ) {
                DropdownMenuTrigger(onClick = { actionsExpanded = true }) {
                    Button(
                        onClick = { actionsExpanded = true },
                        variant = ButtonVariant.Ghost,
                        size = ButtonSize.Icon,
                    ) {
                        Icon(
                            imageVector = Lucide.Ellipsis,
                            contentDescription = stringResource(resource = Res.string.home_projects_item_options_desc),
                            tint = Theme[colors][foreground],
                        )
                    }
                }

                DropdownMenuContent(
                    expanded = actionsExpanded,
                    onDismissRequest = { actionsExpanded = false },
                ) {
                    DownloadedProjectMenuItem(
                        label = Res.string.home_projects_item_menu_open,
                        icon = Lucide.FolderOpen,
                        onClick = {
                            actionsExpanded = false
                            onOpen()
                        },
                    )

                    if (details != null) {
                        DownloadedProjectMenuItem(
                            label = Res.string.home_projects_view_hub,
                            icon = Lucide.Globe,
                            onClick = {
                                actionsExpanded = false
                                onNavigate(
                                    DesktopHubDestination.Project(
                                        username = details.artist.username,
                                        slug = details.slug,
                                    )
                                )
                            },
                        )

                        DownloadedProjectMenuItem(
                            label = Res.string.home_projects_view_artist,
                            icon = Lucide.UserRound,
                            onClick = {
                                actionsExpanded = false
                                onNavigate(DesktopHubDestination.Artist(username = details.artist.username))
                            },
                        )
                    }

                    DropdownMenuSeparator()

                    DownloadedProjectMenuItem(
                        label = Res.string.home_projects_remove_downloaded,
                        icon = Lucide.Trash2,
                        destructive = true,
                        onClick = {
                            actionsExpanded = false
                            onRemove()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadedProjectMenuItem(
    label: StringResource,
    icon: ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    DropdownMenuItem(onClick = onClick, destructive = destructive) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) Theme[colors][dev.anthonyhfm.amethyst.ui.theme.destructive] else Theme[colors][popoverForeground],
            modifier = Modifier
                .size(size = 16.dp),
        )

        Text(text = stringResource(resource = label))
    }
}

@Composable
private fun DownloadedProjectArtwork(
    url: String?,
    fallback: ImageVector,
    modifier: Modifier,
    fallbackSize: Dp = 48.dp,
) {
    val image = rememberHubImage(path = url)

    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Theme[colors][primary].copy(alpha = 0.16f), Theme[colors][muted]),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = fallback,
                contentDescription = null,
                tint = Theme[colors][mutedForeground],
                modifier = Modifier
                    .size(size = fallbackSize),
            )
        }
    }
}
