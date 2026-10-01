package dev.anthonyhfm.amethyst.timeline.ui.pianoroll

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.composeunstyled.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.composables.icons.lucide.*
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.timeline.contract.GridResolution
import dev.anthonyhfm.amethyst.timeline.contract.TimelineEditorTool
import dev.anthonyhfm.amethyst.ui.components.primitives.Button
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonSize
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonVariant
import dev.anthonyhfm.amethyst.ui.theme.border
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.small
import dev.anthonyhfm.amethyst.ui.theme.typography

@Composable
fun PianoRollToolbar(
    activeTool: TimelineEditorTool,
    onToolChange: (TimelineEditorTool) -> Unit,
    gridResolution: GridResolution,
    gridResolutionLocked: Boolean,
    onToggleGridLock: () -> Unit,
    previewEnabled: Boolean,
    onTogglePreview: () -> Unit,
    foldPads: Boolean,
    onToggleFold: () -> Unit,
    followPlayhead: Boolean,
    onToggleFollow: () -> Unit,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomFit: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            ToolButton(
                icon = if (isPlaying) Lucide.Pause else Lucide.Play,
                label = if (isPlaying) "Pause" else "Play",
                selected = isPlaying,
                onClick = onTogglePlayback,
            )
            ToolButton(
                icon = Lucide.Eye,
                label = "Preview",
                selected = previewEnabled,
                onClick = onTogglePreview,
            )
            ToolButton(
                icon = Lucide.ListFilter,
                label = "Fold",
                selected = foldPads,
                onClick = onToggleFold,
            )
            ToolButton(
                icon = Lucide.ArrowRight,
                label = "Follow",
                selected = followPlayhead,
                onClick = onToggleFollow,
            )
            ToolButton(
                icon = Lucide.Pencil,
                label = "Draw  B",
                selected = activeTool == TimelineEditorTool.DRAW,
                onClick = {
                    onToolChange(
                        if (activeTool == TimelineEditorTool.DRAW) TimelineEditorTool.NORMAL
                        else TimelineEditorTool.DRAW
                    )
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onToggleGridLock,
                variant = if (gridResolutionLocked) ButtonVariant.Secondary else ButtonVariant.Ghost,
                size = ButtonSize.Small
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (gridResolutionLocked) Lucide.Lock else Lucide.LockKeyholeOpen,
                        contentDescription = if (gridResolutionLocked) "Fixed grid" else "Adaptive grid",
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = gridResolution.label,
                        style = Theme[typography][small]
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(Theme[colors][border])
            )

            Button(
                onClick = onZoomOut,
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Icon
            ) {
                Icon(
                    imageVector = Lucide.ZoomOut,
                    contentDescription = "Zoom Out",
                    modifier = Modifier.size(14.dp)
                )
            }

            Button(
                onClick = onZoomIn,
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Icon
            ) {
                Icon(
                    imageVector = Lucide.ZoomIn,
                    contentDescription = "Zoom In",
                    modifier = Modifier.size(14.dp)
                )
            }

            Button(
                onClick = onZoomFit,
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Icon
            ) {
                Icon(
                    imageVector = Lucide.Maximize2,
                    contentDescription = "Fit Content",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .semantics { this.selected = selected },
        variant = if (selected) ButtonVariant.Secondary else ButtonVariant.Ghost,
        size = ButtonSize.Small
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(label, style = Theme[typography][small])
        }
    }
}
