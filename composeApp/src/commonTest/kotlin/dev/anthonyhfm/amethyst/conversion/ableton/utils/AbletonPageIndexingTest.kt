package dev.anthonyhfm.amethyst.conversion.ableton.utils

import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertEquals(0, AbletonPageIndexing.sourceOffset(0))
        assertEquals(1, AbletonPageIndexing.sourceOffset(1))
        assertEquals(1, AbletonPageIndexing.sourceOffset(null, true))
        assertEquals(0, AbletonPageIndexing.normalizeSelectorValue(1, 1))
        assertEquals(7, AbletonPageIndexing.normalizeSelectorValue(8, 1))
    }

    @Test
    fun macroValuesScaleOnlyForValidPageRanges() {
        assertEquals(0, AbletonPageIndexing.normalizeMacroValue(1.0, 1, 8, 7))
        assertEquals(7, AbletonPageIndexing.normalizeMacroValue(8.0, 1, 8, 7))
        assertEquals(4, AbletonPageIndexing.normalizeMacroValue(4.5, 1, 8, 7))
        assertEquals(64, AbletonPageIndexing.normalizeMacroValue(63.6, null, 127, 7))
        assertEquals(4, AbletonPageIndexing.normalizeMacroValue(4.0, 5, 5, 7))
        assertEquals(4, AbletonPageIndexing.normalizeMacroValue(4.0, 0, 127, 16))
    }

    @Test
    fun pageCountPrefersKeyRangeAndRejectsInvalidRanges() {
        assertEquals(7, AbletonPageIndexing.pageTargetMaximum(1, 8, 0, 16))
        assertEquals(15, AbletonPageIndexing.pageTargetMaximum(null, null, 1, 16))
        assertEquals(null, AbletonPageIndexing.pageTargetMaximum(0, 127, 2, 16))
        assertEquals(null, AbletonPageIndexing.pageTargetMaximum(5, 5, null, null))
    }
}
