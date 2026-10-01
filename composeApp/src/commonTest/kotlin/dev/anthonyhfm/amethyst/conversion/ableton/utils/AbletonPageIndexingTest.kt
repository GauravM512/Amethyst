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
    fun onlyExactOneBasedSelectorMinimumAddsOffset() {
        assertEquals(expected = 1, actual = AbletonPageIndexing.sourceOffset(selectorMinimum = 1f))
        assertEquals(expected = 0, actual = AbletonPageIndexing.sourceOffset(selectorMinimum = 1.5f))
        assertEquals(expected = 0, actual = AbletonPageIndexing.sourceOffset(selectorMinimum = null))
    }

    private fun pageMaximum(minimum: Float, maximum: Float): Int? =
        AbletonPageIndexing.pageTargetMaximum(
            keyMinimum = null,
            keyMaximum = null,
            controllerMinimum = minimum,
            controllerMaximum = maximum,
        )
}
