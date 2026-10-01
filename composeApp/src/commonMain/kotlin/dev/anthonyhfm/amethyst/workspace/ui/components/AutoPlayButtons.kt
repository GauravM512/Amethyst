package dev.anthonyhfm.amethyst.workspace.ui.components

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composeunstyled.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.BookOpenText
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Square
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.ui.components.primitives.Button
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonSize
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonVariant
import dev.anthonyhfm.amethyst.ui.components.primitives.Tooltip
import dev.anthonyhfm.amethyst.ui.theme.card
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.foreground
import dev.anthonyhfm.amethyst.ui.theme.mutedForeground
import dev.anthonyhfm.amethyst.ui.theme.p
import dev.anthonyhfm.amethyst.ui.theme.primary
import dev.anthonyhfm.amethyst.ui.theme.primaryForeground
import dev.anthonyhfm.amethyst.ui.theme.small
import dev.anthonyhfm.amethyst.ui.theme.typography
import dev.anthonyhfm.amethyst.workspace.AutoPlayRepository
import dev.anthonyhfm.amethyst.workspace.AutoPlayState
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository

@Composable
fun DesktopAutoPlayButtons(
    modifier: Modifier = Modifier,
) {
    val autoPlayState by AutoPlayRepository.state.collectAsState()
    val progress by AutoPlayRepository.progress.collectAsState()
    val totalDuration = AutoPlayRepository.totalDuration
    val hasAutoPlayData = WorkspaceRepository.workspaceMeta?.autoPlay?.actions?.isNotEmpty() == true
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        AutoPlaySettingsDialog(
            onDismiss = { showSettingsDialog = false }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorkspaceToolbarIconButton(
                onClick = {
                    if (autoPlayState == AutoPlayState.LEARNING) {
                        AutoPlayRepository.stopAutoPlay()
                    } else {
                        AutoPlayRepository.startLearningMode()
                    }
                },
                imageVector = Lucide.BookOpenText,
                contentDescription = stringResource(Res.string.workspace_autoplay_learning_mode),
                variant = if (autoPlayState == AutoPlayState.LEARNING) {
                    ButtonVariant.Default
                } else {
                    ButtonVariant.Ghost
                },
                modifier = Modifier
                    .height(28.dp),
            )

            val playDescription = when (autoPlayState) {
                AutoPlayState.STOPPED -> stringResource(Res.string.workspace_autoplay_start)
                AutoPlayState.PLAYING -> stringResource(Res.string.workspace_autoplay_pause)
                AutoPlayState.PAUSED -> stringResource(Res.string.workspace_autoplay_resume)
                AutoPlayState.LEARNING -> stringResource(Res.string.workspace_autoplay_switch_to_normal)
            }

            Tooltip(
                text = playDescription,
                anchor = {
                    Button(
                        onClick = {
                            when (autoPlayState) {
                                AutoPlayState.STOPPED, AutoPlayState.LEARNING -> AutoPlayRepository.startAutoPlay()
                                AutoPlayState.PLAYING -> AutoPlayRepository.pauseAutoPlay()
                                AutoPlayState.PAUSED -> AutoPlayRepository.resumeAutoPlay()
                            }
                        },
                        modifier = Modifier
                            .size(28.dp),
                        size = ButtonSize.Icon,
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = if (autoPlayState == AutoPlayState.PLAYING) {
                                Lucide.Pause
                            } else {
                                Lucide.Play
                            },
                            contentDescription = playDescription,
                            tint = Theme[colors][primaryForeground],
                            modifier = Modifier
                                .size(16.dp),
                        )
                    }
                },
            )

            WorkspaceToolbarIconButton(
                onClick = { AutoPlayRepository.stopAutoPlay() },
                imageVector = Lucide.Square,
                contentDescription = stringResource(Res.string.workspace_autoplay_stop),
                enabled = autoPlayState != AutoPlayState.STOPPED,
                modifier = Modifier
                    .height(28.dp),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            AutoPlayTimeline(
                progress = if (hasAutoPlayData) progress else 0f,
                totalDuration = if (hasAutoPlayData) totalDuration else 0.0,
                enabled = hasAutoPlayData,
                inlineTimes = true,
                onSeek = AutoPlayRepository::seekTo,
            )
        }

        WorkspaceToolbarIconButton(
            onClick = { showSettingsDialog = true },
            imageVector = Lucide.Settings,
            contentDescription = stringResource(Res.string.workspace_autoplay_settings),
        )
    }
}

@Composable
fun MobileAutoPlayButtons() {
    val autoPlayState by AutoPlayRepository.state.collectAsState()
    val progress by AutoPlayRepository.progress.collectAsState()
    val totalDuration = AutoPlayRepository.totalDuration
    var showSettingsDialog by remember { mutableStateOf(false) }

    val hasAutoPlayData = WorkspaceRepository.workspaceMeta?.autoPlay?.actions?.isNotEmpty() == true
    val title = WorkspaceRepository.workspaceMeta?.title?.takeIf { it.isNotBlank() } ?: "AutoPlay"

    if (showSettingsDialog) {
        AutoPlaySettingsDialog(
            onDismiss = { showSettingsDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme[colors][card], RoundedCornerShape(24.dp))
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = Theme[typography][p],
                    color = Theme[colors][foreground],
                    maxLines = 1,
                )
                Text(
                    text = when (autoPlayState) {
                        AutoPlayState.STOPPED -> "Stopped"
                        AutoPlayState.PLAYING -> "Playing"
                        AutoPlayState.PAUSED -> "Paused"
                        AutoPlayState.LEARNING -> "Learning Mode"
                    },
                    style = Theme[typography][small],
                    color = if (autoPlayState == AutoPlayState.LEARNING) Theme[colors][primary] else Theme[colors][mutedForeground],
                )
            }

            WorkspaceToolbarIconButton(
                onClick = { showSettingsDialog = true },
                imageVector = Lucide.Settings,
                contentDescription = stringResource(Res.string.workspace_autoplay_settings),
                variant = ButtonVariant.Ghost,
            )
        }

        AutoPlayTimeline(
            progress = if (hasAutoPlayData) progress else 0f,
            totalDuration = if (hasAutoPlayData) totalDuration else 0.0,
            enabled = hasAutoPlayData,
            compact = true,
            onSeek = AutoPlayRepository::seekTo,
        )

        // Spotify Style Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Learning Mode Toggle Button
            WorkspaceToolbarIconButton(
                onClick = {
                    if (autoPlayState == AutoPlayState.LEARNING) {
                        AutoPlayRepository.stopAutoPlay()
                    } else {
                        AutoPlayRepository.startLearningMode()
                    }
                },
                imageVector = Lucide.BookOpenText,
                contentDescription = if (autoPlayState == AutoPlayState.PLAYING) stringResource(Res.string.workspace_autoplay_switch_to_learning) else stringResource(Res.string.workspace_autoplay_learning_mode),
                variant = if (autoPlayState == AutoPlayState.LEARNING) ButtonVariant.Default else ButtonVariant.Ghost,
            )

            // Prominent Center Spotify Play / Pause Circular Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Theme[colors][primary])
                    .clickable {
                        when (autoPlayState) {
                            AutoPlayState.STOPPED -> AutoPlayRepository.startAutoPlay()
                            AutoPlayState.PLAYING -> AutoPlayRepository.pauseAutoPlay()
                            AutoPlayState.PAUSED -> AutoPlayRepository.resumeAutoPlay()
                            AutoPlayState.LEARNING -> AutoPlayRepository.startAutoPlay()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (autoPlayState == AutoPlayState.PLAYING) Lucide.Pause else Lucide.Play,
                    contentDescription = when (autoPlayState) {
                        AutoPlayState.STOPPED -> stringResource(Res.string.workspace_autoplay_start)
                        AutoPlayState.PLAYING -> stringResource(Res.string.workspace_autoplay_pause)
                        AutoPlayState.PAUSED -> stringResource(Res.string.workspace_autoplay_resume)
                        AutoPlayState.LEARNING -> stringResource(Res.string.workspace_autoplay_switch_to_normal)
                    },
                    tint = Theme[colors][primaryForeground],
                    modifier = Modifier.size(28.dp),
                )
            }

            // Stop Button
            WorkspaceToolbarIconButton(
                onClick = { AutoPlayRepository.stopAutoPlay() },
                imageVector = Lucide.Square,
                contentDescription = stringResource(Res.string.workspace_autoplay_stop),
                enabled = autoPlayState != AutoPlayState.STOPPED,
                variant = ButtonVariant.Ghost,
            )
        }
    }
}
