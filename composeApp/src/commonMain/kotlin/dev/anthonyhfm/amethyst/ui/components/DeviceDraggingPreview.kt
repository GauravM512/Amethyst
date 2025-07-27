package dev.anthonyhfm.amethyst.ui.components

import androidx.compose.runtime.Composable
import dev.anthonyhfm.amethyst.devices.ChainDevice

@Composable
fun DeviceDraggingPreview(
    device: ChainDevice<*>
) {
    device.Content()
}