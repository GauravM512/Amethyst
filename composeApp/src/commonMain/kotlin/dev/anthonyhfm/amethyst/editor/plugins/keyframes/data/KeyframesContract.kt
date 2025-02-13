package dev.anthonyhfm.amethyst.editor.plugins.keyframes.data

import androidx.compose.ui.graphics.Color

interface KeyframesContract {
    sealed interface Event {
        data class SetColor(val x: Int, val y: Int, val color: Color)
    }
}