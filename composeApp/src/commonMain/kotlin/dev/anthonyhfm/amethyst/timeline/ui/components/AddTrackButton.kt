package dev.anthonyhfm.amethyst.timeline.ui.components

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.twotone.Audiotrack
import androidx.compose.material.icons.twotone.Lightbulb
import com.composeunstyled.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.ui.components.ContextMenuItem
import dev.anthonyhfm.amethyst.ui.components.primitives.Button
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonSize
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonVariant
import dev.anthonyhfm.amethyst.ui.components.primitives.DropdownMenu
import dev.anthonyhfm.amethyst.ui.components.primitives.DropdownMenuContent
import dev.anthonyhfm.amethyst.ui.theme.TimelineTheme
import dev.anthonyhfm.amethyst.ui.theme.small
import dev.anthonyhfm.amethyst.ui.theme.typography

@Composable
fun AddTrackButton(
    onAddLightsTrack: () -> Unit = {},
    onAddAudioTrack: () -> Unit = {},
) {
    var showDropdown by remember { mutableStateOf(false) }
    val timelinePalette = TimelineTheme.palette
    val timelineDimensions = TimelineTheme.dimensions

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = timelineDimensions.addTrackHeight)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        DropdownMenu(
            expanded = showDropdown,
            onExpandRequest = {
                SelectionManager.clear()
                showDropdown = true
            },
            onDismissRequest = { showDropdown = false },
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Button(
                onClick = {
                    SelectionManager.clear()
                    showDropdown = !showDropdown
                },
                modifier = Modifier
                    .fillMaxWidth(),
                variant = ButtonVariant.Outline,
                size = ButtonSize.Default,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(resource = Res.string.timeline_add_track_description),
                    tint = timelinePalette.trackHeaderContent,
                )
                Text(
                    text = stringResource(resource = Res.string.timeline_add_track_label),
                    style = Theme[typography][small].copy(color = timelinePalette.trackHeaderContent),
                )
            }

            DropdownMenuContent(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
            ) {
                ContextMenuItem(
                    label = stringResource(resource = Res.string.timeline_add_track_midi),
                    icon = Icons.TwoTone.Lightbulb,
                    onClick = {
                        showDropdown = false
                        onAddLightsTrack()
                    },
                )
                ContextMenuItem(
                    label = stringResource(resource = Res.string.timeline_add_track_audio),
                    icon = Icons.TwoTone.Audiotrack,
                    onClick = {
                        showDropdown = false
                        onAddAudioTrack()
                    },
                )
            }
        }
    }
}
