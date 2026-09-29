package dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton

import kotlin.test.Test
import kotlin.test.assertEquals

class MidiChordShiftTest {
    @Test
    fun chordKeepsOriginalAndOnlyDistinctAdditionalPitches() {
        assertEquals(
            listOf(0, 4, 7, -12),
            chordShifts(listOf(4, 7, 0, 7, -12, 0)),
        )
        assertEquals(listOf(0), chordShifts(List(6) { 0 }))
    }
}
