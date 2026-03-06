package dev.anthonyhfm.amethyst.workspace.chain.ui

import androidx.compose.material.icons.twotone.Adjust
import androidx.compose.material.icons.twotone.AudioFile
import androidx.compose.material.icons.twotone.BlurOn
import androidx.compose.material.icons.twotone.ColorLens
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.Filter
import androidx.compose.material.icons.twotone.FilterTiltShift
import androidx.compose.material.icons.twotone.Flip
import androidx.compose.material.icons.twotone.Gradient
import androidx.compose.material.icons.twotone.Group
import androidx.compose.material.icons.twotone.Layers
import androidx.compose.material.icons.twotone.LineAxis
import androidx.compose.material.icons.twotone.Loop
import androidx.compose.material.icons.twotone.MyLocation
import androidx.compose.material.icons.twotone.Pause
import androidx.compose.material.icons.twotone.Piano
import androidx.compose.material.icons.twotone.RotateLeft
import androidx.compose.material.icons.twotone.Science
import androidx.compose.material.icons.twotone.ShapeLine
import androidx.compose.material.icons.twotone.StopCircle
import androidx.compose.material.icons.twotone.Timeline
import androidx.compose.material.icons.twotone.Timer
import androidx.compose.material.icons.twotone.Transform
import androidx.compose.material.icons.twotone._123
import androidx.compose.runtime.Composable
import dev.anthonyhfm.amethyst.devices.GenericChainDevice
import dev.anthonyhfm.amethyst.devices.audio.clip.ClipChainDevice
import dev.anthonyhfm.amethyst.devices.effects.blur.BlurChainDevice
import dev.anthonyhfm.amethyst.devices.effects.choke.ChokeChainDevice
import dev.anthonyhfm.amethyst.devices.effects.color.ColorChainDevice
import dev.anthonyhfm.amethyst.devices.effects.coordinate_filter.CoordinateFilterChainDevice
import dev.anthonyhfm.amethyst.devices.effects.copy.CopyChainDevice
import dev.anthonyhfm.amethyst.devices.effects.delay.DelayChainDevice
import dev.anthonyhfm.amethyst.devices.effects.flip.FlipChainDevice
import dev.anthonyhfm.amethyst.devices.effects.gradient.GradientChainDevice
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDevice
import dev.anthonyhfm.amethyst.devices.effects.hold.HoldChainDevice
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDevice
import dev.anthonyhfm.amethyst.devices.effects.layer.LayerChainDevice
import dev.anthonyhfm.amethyst.devices.effects.layer_filter.LayerFilterChainDevice
import dev.anthonyhfm.amethyst.devices.effects.loop.LoopChainDevice
import dev.anthonyhfm.amethyst.devices.effects.macro_filter.MacroFilterChainDevice
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDevice
import dev.anthonyhfm.amethyst.devices.effects.offset.OffsetChainDevice
import dev.anthonyhfm.amethyst.devices.effects.pianoroll.PianoRollChainDevice
import dev.anthonyhfm.amethyst.devices.effects.rotate.RotateChainDevice
import dev.anthonyhfm.amethyst.devices.effects.switch.MacroControlChainDevice
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Send
import androidx.compose.material.icons.twotone.Diamond
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Contrast
import androidx.compose.material.icons.twotone.Preview
import dev.anthonyhfm.amethyst.devices.effects.color_filter.ColorFilterChainDevice
import dev.anthonyhfm.amethyst.devices.effects.preview.PreviewChainDevice
import dev.anthonyhfm.amethyst.devices.effects.shift.ShiftChainDevice
import dev.anthonyhfm.amethyst.devices.effects.transmit.TransmitChainDevice
import dev.anthonyhfm.amethyst.gem.ui.editor.GemEditorWorkspaceMode
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import dev.anthonyhfm.amethyst.ui.components.AmethystContextMenu
import dev.anthonyhfm.amethyst.ui.components.ContextMenuItem
import dev.anthonyhfm.amethyst.ui.components.ContextMenuSubmenuItem

@Composable
fun ChainDevicePicker(
    visible: Boolean,
    sampling: Boolean,
    onPickComponent: (GenericChainDevice<*>) -> Unit,
    onDismiss: () -> Unit
) {
    AmethystContextMenu(
        expanded = visible,
        onDismissRequest = onDismiss
    ) { onNavigate, _, level ->
        if (!sampling) {
            // Lights Menu
            when (level) {
                "main" -> {
                    ContextMenuSubmenuItem("Container", icon = Icons.TwoTone.Group, onClick = { onNavigate("container") })
                    ContextMenuSubmenuItem("Filter", icon = Icons.TwoTone.Filter, onClick = { onNavigate("filter") })
                    ContextMenuSubmenuItem("Color", icon = Icons.TwoTone.ColorLens, onClick = { onNavigate("color") })
                    ContextMenuSubmenuItem("Shape", icon = Icons.TwoTone.ShapeLine, onClick = { onNavigate("shape") })
                    ContextMenuSubmenuItem("Timing", icon = Icons.TwoTone.Timer, onClick = { onNavigate("timing") })
                    ContextMenuSubmenuItem("Transform", icon = Icons.TwoTone.Transform, onClick = { onNavigate("transform") })
                    ContextMenuSubmenuItem("Effects", icon = Icons.TwoTone.Science, onClick = { onNavigate("effects") })
                    ContextMenuSubmenuItem("Misc", icon = Icons.TwoTone.Adjust, onClick = { onNavigate("misc") })
                    ContextMenuSubmenuItem("Gems", icon = Icons.TwoTone.Diamond, onClick = { onNavigate("gems") })
                }
                "gems" -> {
                    ContextMenuItem("New Gem", icon = Icons.TwoTone.Add, onClick = { WorkspaceRepository.switchMode(GemEditorWorkspaceMode()) })
                }
                "container" -> {
                    ContextMenuItem("Group", icon = Icons.TwoTone.Group, onClick = { onPickComponent(GroupChainDevice()) })
                    ContextMenuItem("Choke", icon = Icons.TwoTone.StopCircle, onClick = { onPickComponent(ChokeChainDevice()) })
                    ContextMenuItem("Multi", icon = Icons.TwoTone._123, onClick = { onPickComponent(MultiGroupChainDevice()) })
                }
                "filter" -> {
                    ContextMenuItem("Coordinate Filter", icon = Icons.TwoTone.MyLocation, onClick = { onPickComponent(CoordinateFilterChainDevice()) })
                    ContextMenuItem("Layer Filter", icon = Icons.TwoTone.Layers, onClick = { onPickComponent(LayerFilterChainDevice()) })
                    ContextMenuItem("Macro Filter", icon = Icons.TwoTone.FilterTiltShift, onClick = { onPickComponent(MacroFilterChainDevice()) })
                    ContextMenuItem("Color Filter", icon = Icons.TwoTone.ColorLens, onClick = { onPickComponent(ColorFilterChainDevice()) })
                }
                "color" -> {
                    ContextMenuItem("Color", icon = Icons.TwoTone.ColorLens, onClick = { onPickComponent(ColorChainDevice()) })
                    ContextMenuItem("Gradient", icon = Icons.TwoTone.Gradient, onClick = { onPickComponent(GradientChainDevice()) })
                    ContextMenuItem("Shift", icon = Icons.TwoTone.Contrast, onClick = { onPickComponent(ShiftChainDevice()) })
                }
                "shape" -> {
                    ContextMenuItem("Copy", icon = Icons.TwoTone.ContentCopy, onClick = { onPickComponent(CopyChainDevice()) })
                    ContextMenuItem("Keyframes", icon = Icons.TwoTone.Timeline, onClick = { onPickComponent(KeyframesChainDevice()) })
                    ContextMenuItem("Piano Roll", icon = Icons.TwoTone.Piano, onClick = { onPickComponent(PianoRollChainDevice()) })
                }
                "timing" -> {
                    ContextMenuItem("Delay", icon = Icons.TwoTone.Timer, onClick = { onPickComponent(DelayChainDevice()) })
                    ContextMenuItem("Hold", icon = Icons.TwoTone.Pause, onClick = { onPickComponent(HoldChainDevice()) })
                    ContextMenuItem("Loop", icon = Icons.TwoTone.Loop, onClick = { onPickComponent(LoopChainDevice()) })
                }
                "transform" -> {
                    ContextMenuItem("Offset", icon = Icons.TwoTone.LineAxis, onClick = { onPickComponent(OffsetChainDevice()) })
                    ContextMenuItem("Layer", icon = Icons.TwoTone.Layers, onClick = { onPickComponent(LayerChainDevice()) })
                    ContextMenuItem("Flip", icon = Icons.TwoTone.Flip, onClick = { onPickComponent(FlipChainDevice()) })
                    ContextMenuItem("Rotate", icon = Icons.TwoTone.RotateLeft, onClick = { onPickComponent(RotateChainDevice()) })
                }
                "effects" -> {
                    ContextMenuItem("Blur", icon = Icons.TwoTone.BlurOn, onClick = { onPickComponent(BlurChainDevice()) })
                }
                "misc" -> {
                    ContextMenuItem("Macro Control", icon = Icons.TwoTone.Adjust, onClick = { onPickComponent(MacroControlChainDevice()) })
                    ContextMenuItem("Preview", icon = Icons.TwoTone.Preview, onClick = { onPickComponent(PreviewChainDevice()) })
                    ContextMenuItem("Transmit", icon = Icons.AutoMirrored.TwoTone.Send, onClick = { onPickComponent(TransmitChainDevice()) })
                }
            }
        } else {
            // Sampling Menu
            when (level) {
                "main" -> {
                    ContextMenuSubmenuItem("Container", icon = Icons.TwoTone.Group, onClick = { onNavigate("container") })
                    ContextMenuItem("Clip", icon = Icons.TwoTone.AudioFile, onClick = { onPickComponent(ClipChainDevice()) })
                    ContextMenuSubmenuItem("Filter", icon = Icons.TwoTone.Filter, onClick = { onNavigate("filter") })
                    ContextMenuSubmenuItem("Timing", icon = Icons.TwoTone.Timer, onClick = { onNavigate("timing") })
                    ContextMenuSubmenuItem("Gems", icon = Icons.TwoTone.Diamond, onClick = { onNavigate("gems") })
                    ContextMenuSubmenuItem("Misc", icon = Icons.TwoTone.Adjust, onClick = { onNavigate("misc") })
                }
                "gems" -> {
                    ContextMenuItem("New Gem", icon = Icons.TwoTone.Add, onClick = { WorkspaceRepository.switchMode(GemEditorWorkspaceMode()) })
                }
                "container" -> {
                    ContextMenuItem("Group", icon = Icons.TwoTone.Group, onClick = { onPickComponent(GroupChainDevice()) })
                    ContextMenuItem("Multi", icon = Icons.TwoTone._123, onClick = { onPickComponent(MultiGroupChainDevice()) })
                }
                "filter" -> {
                    ContextMenuItem("Coordinate Filter", icon = Icons.TwoTone.MyLocation, onClick = { onPickComponent(CoordinateFilterChainDevice()) })
                    ContextMenuItem("Macro Filter", icon = Icons.TwoTone.FilterTiltShift, onClick = { onPickComponent(MacroFilterChainDevice()) })
                }
                "timing" -> {
                    ContextMenuItem("Delay", icon = Icons.TwoTone.Timer, onClick = { onPickComponent(DelayChainDevice()) })
                    ContextMenuItem("Hold", icon = Icons.TwoTone.Pause, onClick = { onPickComponent(HoldChainDevice()) })
                    ContextMenuItem("Loop", icon = Icons.TwoTone.Loop, onClick = { onPickComponent(LoopChainDevice()) })
                }
                "misc" -> {
                    ContextMenuItem("Macro Control", icon = Icons.TwoTone.Adjust, onClick = { onPickComponent(MacroControlChainDevice()) })
                }
            }
        }
    }
}

private fun getLightsMenu() {}
private fun getSamplingMenu() {}