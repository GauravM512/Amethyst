package dev.anthonyhfm.amethyst.conversion.ableton.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AbletonPageIndexingTest {

    @Test
    fun pageControlsRecognizeMappingsAndBoundedSelectors() {
        assertTrue(AbletonPageIndexing.controlsPages(true, emptyList()))
        assertTrue(AbletonPageIndexing.controlsPages(false, listOf(1 to 16)))
        assertTrue(AbletonPageIndexing.controlsPages(false, listOf(0 to 127, 0 to 7)))
        assertFalse(AbletonPageIndexing.controlsPages(false, emptyList()))
        assertFalse(AbletonPageIndexing.controlsPages(false, listOf(0 to 127, 0 to 0)))
    }

    @Test
    fun oneBasedSelectorsBecomeZeroBasedPages() {
        assertEquals(0, AbletonPageIndexing.sourceOffset(selectorMinimum = 0f))
        assertEquals(1, AbletonPageIndexing.sourceOffset(selectorMinimum = 1f))
        assertEquals(0, AbletonPageIndexing.sourceOffset(selectorMinimum = null))
        assertEquals(0, AbletonPageIndexing.normalizeSelectorValue(1, 1))
        assertEquals(7, AbletonPageIndexing.normalizeSelectorValue(8, 1))
    }

    @Test
    fun macroValuesScaleOnlyForValidPageRanges() {
        assertEquals(0, AbletonPageIndexing.normalizeMacroValue(1.0, 1f, 8f, 7))
        assertEquals(7, AbletonPageIndexing.normalizeMacroValue(8.0, 1f, 8f, 7))
        assertEquals(4, AbletonPageIndexing.normalizeMacroValue(4.5, 1f, 8f, 7))
        assertEquals(64, AbletonPageIndexing.normalizeMacroValue(63.6, null, 127f, 7))
        assertEquals(4, AbletonPageIndexing.normalizeMacroValue(4.0, 5f, 5f, 7))
        assertEquals(4, AbletonPageIndexing.normalizeMacroValue(4.0, 0f, 127f, 16))
    }

    @Test
    fun pageCountPrefersKeyRangeAndRejectsInvalidRanges() {
        assertEquals(7, AbletonPageIndexing.pageTargetMaximum(1, 8, 0f, 16f))
        assertEquals(15, AbletonPageIndexing.pageTargetMaximum(null, null, 1f, 16f))
        assertEquals(14, AbletonPageIndexing.pageTargetMaximum(0, 127, 2f, 16f))
        assertEquals(null, AbletonPageIndexing.pageTargetMaximum(5, 5, null, null))
    }
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
