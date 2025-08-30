package dev.anthonyhfm.amethyst.core.engine.heaven

import androidx.compose.ui.graphics.Color

data class RawLEDUpdate(var index: Byte, var color: Color) {
    fun offset(offset: Int) {
        index = (index + offset).toByte()
    }

    constructor(index: Int, color: Color) : this(index.toByte(), color.copy())

    constructor(n: RawLEDUpdate, offset: Int) : this((n.index + offset).toByte(), n.color.copy())
}