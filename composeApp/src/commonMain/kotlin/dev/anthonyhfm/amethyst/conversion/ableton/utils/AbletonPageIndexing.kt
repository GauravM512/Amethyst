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
        hasOneBasedPageController: Boolean = false,
    ): Int {
        return if (hasOneBasedPageController || selectorMinimum == 1f) {
            1
        } else {
            0
        }
    }

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
            (controllerMinimum == 0f || controllerMinimum == 1f) &&
            controllerMaximum in 1f..16f && controllerMaximum == controllerMaximum.toInt().toFloat()
        ) {
            (controllerMaximum - controllerMinimum).toInt().takeIf { it in 1..15 }
        } else {
            null
        }
    }
}
