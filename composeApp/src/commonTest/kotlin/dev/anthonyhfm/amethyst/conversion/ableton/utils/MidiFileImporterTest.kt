package dev.anthonyhfm.amethyst.conversion.ableton.utils

import androidx.compose.ui.unit.IntOffset
import dev.anthonyhfm.amethyst.core.util.Timing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds

class MidiFileImporterTest {
    private val target = MidiFileImporter.DeviceTarget("test-pad", IntOffset(10, 0))
    private val palette = arrayOf(Triple(0, 0, 0), Triple(63, 0, 31))

    private fun midi(vararg events: Int, format: Int = 0, tracks: Int = 1): ByteArray {
        val payload = events.map(Int::toByte).toByteArray()
        val length = payload.size
        return byteArrayOf(
            0x4d, 0x54, 0x68, 0x64, 0, 0, 0, 6,
            0, format.toByte(), 0, tracks.toByte(), 0, 96,
            0x4d, 0x54, 0x72, 0x6b,
            (length ushr 24).toByte(), (length ushr 16).toByte(), (length ushr 8).toByte(), length.toByte(),
        ) + payload
    }

    @Test
    fun rejectsTruncatedHeadersAndUnsupportedMidiFormats() {
        assertTrue(MidiFileImporter.loadData(byteArrayOf(), launchpad = target).frames.size == 1)
        assertTrue(MidiFileImporter.loadData("bad header data".encodeToByteArray(), launchpad = target).frames.size == 1)
        assertTrue(MidiFileImporter.loadData(midi(0, 0xff, 0x2f, 0, format = 1), launchpad = target).frames.size == 1)
        assertTrue(MidiFileImporter.loadData(midi(0, 0xff, 0x2f, 0, tracks = 2), launchpad = target).frames.size == 1)
    }

    @Test
    fun noteOnUsesPaletteAndLaunchpadOffsetThenNoteOffClearsPad() {
        val result = MidiFileImporter.loadData(
            midi(0, 0x90, 36, 1, 96, 0x80, 36, 0, 0, 0xff, 0x2f, 0),
            palette = palette,
            launchpad = target,
        )
        val lit = result.frames.first().entries.single()
        assertEquals("test-pad", lit.launchpadId)
        assertEquals(lit.localX!! + 10, lit.x)
        assertEquals(lit.localY, lit.y)
        assertEquals(1f, lit.r)
        assertEquals(0f, lit.g)
        assertEquals(31f / 63f, lit.b)
        assertTrue(result.frames.last().entries.isEmpty())
    }

    @Test
    fun zeroVelocityNoteOnAndRunningStatusClearPreviousColor() {
        val result = MidiFileImporter.loadData(
            midi(0, 0x90, 36, 1, 96, 36, 0, 0, 0xff, 0x2f, 0),
            palette = palette,
            launchpad = target,
        )
        assertEquals(1, result.frames.first().entries.size)
        assertTrue(result.frames.last().entries.isEmpty())
    }

    @Test
    fun outOfRangeNotesDoNotCreateLightsAndVelocitiesClampToPalette() {
        val result = MidiFileImporter.loadData(
            midi(0, 0x90, 127, 1, 0, 0x90, 36, 127, 0, 0xff, 0x2f, 0),
            palette = palette,
            launchpad = target,
        )
        assertEquals(1, result.frames.single().entries.size)
        assertEquals(1f, result.frames.single().entries.single().r)
    }

    @Test
    fun quarterNoteDurationUsesRequestedBpm() {
        val result = MidiFileImporter.loadData(
            midi(0, 0x90, 36, 1, 96, 0x80, 36, 0, 0, 0xff, 0x2f, 0),
            bpm = 120.0,
            palette = palette,
            launchpad = target,
        )
        assertEquals(500.milliseconds, assertIs<Timing.Duration>(result.frames.first().timing).duration)
    }

    @Test
    fun embeddedTempoOverridesProjectBpm() {
        val result = MidiFileImporter.loadData(
            midi(0, 0xff, 0x51, 3, 0x0f, 0x42, 0x40, 0, 0x90, 36, 1,
                96, 0x80, 36, 0, 0, 0xff, 0x2f, 0),
            bpm = 120.0,
            palette = palette,
            launchpad = target,
        )
        assertEquals(1000.milliseconds, assertIs<Timing.Duration>(result.frames.first().timing).duration)
    }
}
