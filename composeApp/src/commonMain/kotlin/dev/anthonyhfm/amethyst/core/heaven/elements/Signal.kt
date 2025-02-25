package dev.anthonyhfm.amethyst.core.heaven.elements

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.midi.devices.LaunchpadDevice

data class Signal(
    val origin: Any?,
    val device: LaunchpadDevice?,
    val x: Int,
    val y: Int,
    val color: Color,
    val layer: Int = 0,
    val blendingMode: BlendingMode = BlendingMode.NORMAL
)

enum class BlendingMode {
    NORMAL, MULTIPLY, ADD, MASK
}
