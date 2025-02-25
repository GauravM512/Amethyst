package dev.anthonyhfm.amethyst.core.heaven.elements

import androidx.compose.ui.graphics.Color

class RawUpdate(var index: Byte, var color: Color) {
    fun offset(offset: Int) {
        index = (index + offset).toByte()
    }

    fun clone(): RawUpdate {
        return RawUpdate(index, color.copy())
    }

    constructor(index: Int, color: Color) : this(index.toByte(), color.copy())

    constructor(n: RawUpdate, offset: Int) : this((n.index + offset).toByte(), n.color.copy())
}