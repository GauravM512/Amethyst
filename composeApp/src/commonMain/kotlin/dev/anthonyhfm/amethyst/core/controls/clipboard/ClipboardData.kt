package dev.anthonyhfm.amethyst.core.controls.clipboard

import dev.anthonyhfm.amethyst.core.controls.selection.Selectable
import dev.anthonyhfm.amethyst.devices.DeviceState

sealed interface ClipboardData {
    data class ChainDevice(
        val states: List<DeviceState>
    ) : ClipboardData

    data class GradientStep(
        val step: Selectable.GradientStep
    ) : ClipboardData
}