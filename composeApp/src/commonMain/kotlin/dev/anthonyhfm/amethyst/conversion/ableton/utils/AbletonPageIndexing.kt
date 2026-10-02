package dev.anthonyhfm.amethyst.conversion.ableton.utils

import kotlin.math.roundToInt

internal object AbletonPageIndexing {
    fun controlsPages(
        hasKeyMidiMapping: Boolean,
        selectorRanges: Iterable<Pair<Int, Int>>,
    ): Boolean = hasKeyMidiMapping || selectorRanges.any { (minimum, maximum) ->
        minimum != 0 || (maximum != 0 && maximum != 127)
    }

    fun sourceOffset(
        selectorMinimum: Float?,
    ): Int {
        return if (selectorMinimum != null && isSelectorValue(value = selectorMinimum)) {
            selectorMinimum.toInt()
        } else {
            0
        }
    }

    private fun isSelectorValue(value: Float): Boolean =
        value in 0f..127f && value == value.toInt().toFloat()

    fun normalizeSelectorValue(value: Int, sourceOffset: Int): Int =
        value - sourceOffset

    fun normalizeMacroValue(
        value: Double,
        sourceMinimum: Float?,
        sourceMaximum: Float?,
        targetMaximum: Int?,
    ): Int {
        if (
            sourceMinimum == null || sourceMaximum == null || targetMaximum == null ||
            !sourceMinimum.isFinite() || !sourceMaximum.isFinite() ||
            sourceMaximum <= sourceMinimum || targetMaximum !in 1..15
        ) {
            return value.roundToInt()
        }

        return (
            (value - sourceMinimum) * targetMaximum.toDouble() /
                (sourceMaximum.toDouble() - sourceMinimum.toDouble())
            ).roundToInt()
    }

    fun pageTargetMaximum(
        keyMinimum: Int?,
        keyMaximum: Int?,
        controllerMinimum: Float?,
        controllerMaximum: Float?,
    ): Int? {
        val keyRangeSize = if (keyMinimum != null && keyMaximum != null) {
            (keyMaximum - keyMinimum).takeIf { it in 1..15 }
        } else {
            null
        }
        if (keyRangeSize != null) {
            return keyRangeSize
        }

        return if (
            controllerMinimum != null && controllerMaximum != null &&
            isSelectorValue(value = controllerMinimum) && isSelectorValue(value = controllerMaximum)
        ) {
            (controllerMaximum - controllerMinimum).toInt().takeIf { it in 1..15 }
        } else {
            null
        }
    }
}
