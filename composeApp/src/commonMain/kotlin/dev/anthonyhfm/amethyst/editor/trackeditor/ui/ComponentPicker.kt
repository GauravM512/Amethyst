package dev.anthonyhfm.amethyst.editor.trackeditor.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.anthonyhfm.amethyst.editor.plugins.EffectDevice
import dev.anthonyhfm.amethyst.editor.plugins.color.ColorEffectDevice
import dev.anthonyhfm.amethyst.editor.plugins.delay.DelayEffectDevice
import dev.anthonyhfm.amethyst.editor.plugins.filter.FilterEffectDevice
import dev.anthonyhfm.amethyst.editor.plugins.gradient.GradientEffectDevice
import dev.anthonyhfm.amethyst.editor.plugins.group.GroupEffectDevice
import dev.anthonyhfm.amethyst.editor.plugins.keyframes.KeyframesEffectDevice
import dev.anthonyhfm.amethyst.editor.plugins.offset.OffsetEffectDevice
import dev.anthonyhfm.amethyst.editor.plugins.preview.PreviewEffectDevice

data class PickableComponent(
    val name: String,
    val icon: ImageVector,
    val plugin: EffectDevice
)

@Composable
fun ComponentPicker(
    visible: Boolean,
    onPickComponent: (EffectDevice) -> Unit,
    onDismiss: () -> Unit
) {
    val pickableComponents: Array<PickableComponent> = arrayOf(
        PickableComponent(
            name = "Group",
            icon = Icons.AutoMirrored.Filled.List,
            plugin = GroupEffectDevice()
        ),
        PickableComponent(
            name = "Filter",
            icon = Icons.Default.Filter,
            plugin = FilterEffectDevice()
        ),
        PickableComponent(
            name = "Offset",
            icon = Icons.Default.GridOn,
            plugin = OffsetEffectDevice()
        ),
        PickableComponent(
            name = "Delay",
            icon = Icons.Default.MoreTime,
            plugin = DelayEffectDevice()
        ),
        PickableComponent(
            name = "Color",
            icon = Icons.Default.ColorLens,
            plugin = ColorEffectDevice()
        ),
        PickableComponent(
            name = "Gradient",
            icon = Icons.Default.Gradient,
            plugin = GradientEffectDevice()
        ),
        PickableComponent(
            name = "Keyframes",
            icon = Icons.Default.Animation,
            plugin = KeyframesEffectDevice()
        ),
        PickableComponent(
            name = "Preview",
            icon = Icons.Default.Preview,
            plugin = PreviewEffectDevice()
        ),
    )

    DropdownMenu(
        expanded = visible,
        onDismissRequest = {
            onDismiss()
        }
    ) {
        pickableComponents.forEach {
            DropdownMenuItem(
                text = {
                    Text(it.name)
                },
                leadingIcon = {
                    Icon(
                        imageVector = it.icon,
                        contentDescription = null
                    )
                },
                onClick = {
                    onDismiss()

                    onPickComponent(it.plugin)
                }
            )
        }
    }
}