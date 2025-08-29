package dev.anthonyhfm.amethyst.ui.components

import androidx.compose.runtime.Composable
import dev.anthonyhfm.amethyst.devices.GenericChainDevice

@Composable
fun DeviceDraggingPreview(
    device: GenericChainDevice<*>
) {
    device.Content()
}