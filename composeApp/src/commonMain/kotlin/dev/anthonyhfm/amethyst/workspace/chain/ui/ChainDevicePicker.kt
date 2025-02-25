package dev.anthonyhfm.amethyst.workspace.chain.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.anthonyhfm.amethyst.devices.ChainDevice

data class PickableComponent(
    val name: String,
    val icon: ImageVector,
    val plugin: ChainDevice<*>
)

@Composable
fun ChainDevicePicker(
    visible: Boolean,
    onPickComponent: (ChainDevice<*>) -> Unit,
    onDismiss: () -> Unit
) {
    val pickableComponents: Array<PickableComponent> = arrayOf(
        /*PickableComponent(
            name = "Offset",
            icon = Icons.Default.GridOn,
            plugin = OffsetEffectDevice()
        ),*/
        /*PickableComponent(
            name = "Delay",
            icon = Icons.Default.MoreTime,
            plugin = DelayChainDevice()
        ),*/
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