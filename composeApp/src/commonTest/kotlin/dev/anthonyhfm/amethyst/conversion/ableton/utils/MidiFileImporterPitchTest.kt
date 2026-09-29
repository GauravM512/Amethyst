package dev.anthonyhfm.amethyst.conversion.ableton.utils

import androidx.compose.ui.unit.IntOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MidiFileImporterPitchTest {
    @Test
    fun importedMidiKeepsHiddenAndVisiblePitchesDistinct() {
        val track = listOf(
            0, 0x90, 26, 5,
            0, 0x90, 36, 6,
            96, 0x80, 26, 0,
            0, 0x80, 36, 0,
            0, 0xFF, 0x2F, 0,
        )
        val header = listOf(
            0x4D, 0x54, 0x68, 0x64,
            0, 0, 0, 6,
            0, 0,
            0, 1,
            0, 96,
            0x4D, 0x54, 0x72, 0x6B,
            0, 0, 0, track.size,
        )
        val data = (header + track).map(Int::toByte).toByteArray()

        val result = MidiFileImporter.loadData(
            data = data,
            launchpad = MidiFileImporter.DeviceTarget(
                launchpadId = "right",
                offset = IntOffset(x = 10, y = 0),
            ),
        )

        val entries = result.frames.first().entries
        assertEquals(setOf(26, 36), entries.mapNotNull { it.abletonPitch }.toSet())
        assertTrue(entries.any { it.abletonPitch == 26 && it.localX!! < 0 })
        assertTrue(entries.any { it.abletonPitch == 36 && it.localX == 1 && it.localY == 8 })
    }
}
