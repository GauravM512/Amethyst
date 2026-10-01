package dev.anthonyhfm.amethyst.conversion.ableton.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AbletonPageIndexingTest {
    @Test
    fun fractionalControllerRangeNormalizesToPages() {
        assertEquals(
            expected = 7,
            actual = AbletonPageIndexing.normalizeMacroValue(
                value = 50.5,
                sourceMinimum = 0.5f,
                sourceMaximum = 100.5f,
                targetMaximum = 14,
            ),
        )
    }

    @Test
    fun onlyDiscreteControllerRangesIdentifyPages() {
        assertEquals(expected = 15, actual = pageMaximum(minimum = 1f, maximum = 16f))
        assertEquals(expected = 15, actual = pageMaximum(minimum = 0f, maximum = 15f))
        assertEquals(expected = 15, actual = pageMaximum(minimum = 5f, maximum = 20f))
        assertEquals(expected = 15, actual = pageMaximum(minimum = 112f, maximum = 127f))
        assertNull(actual = pageMaximum(minimum = -1f, maximum = 14f))
        assertNull(actual = pageMaximum(minimum = 113f, maximum = 128f))
        assertNull(actual = pageMaximum(minimum = 5f, maximum = 5f))
        assertNull(actual = pageMaximum(minimum = 5f, maximum = 4f))
        assertNull(actual = pageMaximum(minimum = 5f, maximum = 21f))
        assertNull(actual = pageMaximum(minimum = 0.5f, maximum = 15f))
        assertNull(actual = pageMaximum(minimum = 0f, maximum = 15.5f))
        assertNull(actual = pageMaximum(minimum = 0f, maximum = 57.109726f))
    }

    @Test
    fun explicitKeyRangeSupportsFractionalControllerEndpoints() {
        assertEquals(
            expected = 15,
            actual = AbletonPageIndexing.pageTargetMaximum(
                keyMinimum = 0,
                keyMaximum = 15,
                controllerMinimum = 0.5f,
                controllerMaximum = 57.109726f,
            ),
        )
    }

    @Test
    fun selectorMinimumDefinesAnyValidPageBase() {
        for (minimum in listOf(0, 1, 5, 32, 112, 127)) {
            val offset = AbletonPageIndexing.sourceOffset(selectorMinimum = minimum.toFloat())
            assertEquals(expected = minimum, actual = offset)
            assertEquals(
                expected = 0,
                actual = AbletonPageIndexing.normalizeSelectorValue(value = minimum, sourceOffset = offset),
            )
        }
        assertEquals(expected = 0, actual = AbletonPageIndexing.sourceOffset(selectorMinimum = 1.5f))
        assertEquals(expected = 0, actual = AbletonPageIndexing.sourceOffset(selectorMinimum = -1f))
        assertEquals(expected = 0, actual = AbletonPageIndexing.sourceOffset(selectorMinimum = 128f))
        assertEquals(expected = 0, actual = AbletonPageIndexing.sourceOffset(selectorMinimum = Float.NaN))
        assertEquals(expected = 0, actual = AbletonPageIndexing.sourceOffset(selectorMinimum = Float.POSITIVE_INFINITY))
        assertEquals(expected = 0, actual = AbletonPageIndexing.sourceOffset(selectorMinimum = null))
    }

    @Test
    fun shiftedMacroRangesNormalizeToTheSamePages() {
        for (minimum in listOf(0, 1, 5, 32, 112)) {
            val maximum = minimum + 15
            val targetMaximum = pageMaximum(minimum = minimum.toFloat(), maximum = maximum.toFloat())

            for (page in 0..15) {
                assertEquals(
                    expected = page,
                    actual = AbletonPageIndexing.normalizeMacroValue(
                        value = (minimum + page).toDouble(),
                        sourceMinimum = minimum.toFloat(),
                        sourceMaximum = maximum.toFloat(),
                        targetMaximum = targetMaximum,
                    ),
                )
            }
        }
    }

    private fun pageMaximum(minimum: Float, maximum: Float): Int? =
        AbletonPageIndexing.pageTargetMaximum(
            keyMinimum = null,
            keyMaximum = null,
            controllerMinimum = minimum,
            controllerMaximum = maximum,
        )
}
