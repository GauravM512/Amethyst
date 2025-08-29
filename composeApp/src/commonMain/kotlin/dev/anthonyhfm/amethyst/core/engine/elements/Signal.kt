package dev.anthonyhfm.amethyst.core.engine.elements

import androidx.compose.ui.graphics.Color

sealed interface Signal {
    val origin: Any?

    data class LED(
        override val origin: Any?,
        val x: Int,
        val y: Int,
        val color: Color,
        val layer: Int = 0,
        val blendingMode: BlendingMode = BlendingMode.Normal
    ) : Signal {
        enum class BlendingMode {
            Normal, Multiply, Add, Mask
        }
    }

    data class Midi(
        override val origin: Any?,
        val x: Int,
        val y: Int,
        val velocity: Int,
    ) : Signal

    data class AudioSignal(
        override val origin: Any?,
    ) : Signal
}