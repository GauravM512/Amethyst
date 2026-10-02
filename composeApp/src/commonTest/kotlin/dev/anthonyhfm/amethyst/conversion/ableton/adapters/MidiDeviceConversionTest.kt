package dev.anthonyhfm.amethyst.conversion.ableton.adapters

import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.*
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonOn
import dev.anthonyhfm.amethyst.core.util.Timing
import dev.anthonyhfm.amethyst.core.util.Palettes
import dev.anthonyhfm.amethyst.devices.ableton.AbletonArpeggiatorChainDeviceState
import dev.anthonyhfm.amethyst.devices.ableton.AbletonChordChainDeviceState
import dev.anthonyhfm.amethyst.devices.ableton.AbletonPitcherChainDeviceState
import dev.anthonyhfm.amethyst.devices.ableton.AbletonVelocityChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.hold.HoldChainDeviceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class MidiDeviceConversionTest {
    @Test
    fun pitcherAndVelocityKeepNativeMidiParameters() {
        val pitch = MidiPitcher(1, pitch = MidiPitcher.Pitch(AbletonManual(-12)))
        val pitcher = assertIs<AbletonPitcherChainDeviceState>(AbletonAdapter.resolveAdapter(pitch)!!.toDeviceStates().single())
        assertEquals(-12, pitcher.pitch)

        val velocity = MidiVelocity(2, maxOut = MidiVelocity.MaxOut(AbletonManual(1)))
        val converted = assertIs<AbletonVelocityChainDeviceState>(AbletonAdapter.resolveAdapter(velocity)!!.toDeviceStates().single())
        assertEquals(expected = 1, actual = converted.outLow)
        assertEquals(expected = 1, actual = converted.outHigh)
        val expectedPalette = AbletonConverter.palette.takeUnless { it.contentEquals(Palettes.novation) }
            ?.map { (red, green, blue) -> (red shl 12) or (green shl 6) or blue }
        assertEquals(expected = expectedPalette, actual = converted.palette)
    }

    @Test
    fun chordKeepsSixIndependentShifts() {
        val shifts = (1..6).map { MidiChord.Shift(AbletonManual(it * 2)) }
        val chord = MidiChord(1, shift1 = shifts[0], shift2 = shifts[1], shift3 = shifts[2],
            shift4 = shifts[3], shift5 = shifts[4], shift6 = shifts[5])
        val converted = assertIs<AbletonChordChainDeviceState>(AbletonAdapter.resolveAdapter(chord)!!.toDeviceStates().single())
        assertEquals(expected = listOf(0) + (1..6).map { it * 2 }, actual = converted.shifts)
    }

    @Test
    fun noteLengthConvertsFreeTimeGateAndReleaseMode() {
        val noteLength = MidiNoteLength(
            id = 1,
            mode = MidiNoteLength.Mode(AbletonManual(true)),
            syncState = MidiNoteLength.SyncState(AbletonManual(false)),
            timeLength = MidiNoteLength.TimeLength(AbletonManual(250.0)),
            syncedLength = MidiNoteLength.SyncedLength(AbletonManual(4)),
            gate = MidiNoteLength.Gate(AbletonManual(100)),
        )
        val result = assertIs<HoldChainDeviceState>(AbletonAdapter.resolveAdapter(noteLength)!!.toDeviceStates().single())
        assertTrue(result.onRelease)
        assertEquals(250.milliseconds, assertIs<Timing.Duration>(result.timing).duration)
        assertEquals(0.5f, result.gate)
    }

    @Test
    fun arpeggiatorKeepsStepsRateGateAndMute() {
        val arp = MidiArpeggiator(
            id = 1, on = AbletonOn(AbletonManual(false)),
            transposeDistance = MidiArpeggiator.TransposeDistance(AbletonManual(7)),
            transposeSteps = MidiArpeggiator.TransposeSteps(AbletonManual(3)),
            syncState = MidiArpeggiator.SyncState(AbletonManual(false)),
            syncRate = MidiArpeggiator.SyncedRate(AbletonManual(4)),
            repeatCount = MidiArpeggiator.RepeatCount(AbletonManual(2)),
            freeRate = MidiArpeggiator.FreeRate(AbletonManual(125f)),
            gate = MidiArpeggiator.Gate(AbletonManual(0.75f)),
            velocityEnabled = MidiArpeggiator.VelocitySwitch(AbletonManual(false)),
            velocityTarget = MidiArpeggiator.VelocityTarget(AbletonManual(1)),
        )
        val result = assertIs<AbletonArpeggiatorChainDeviceState>(AbletonAdapter.resolveAdapter(arp)!!.toDeviceStates().single())
        assertEquals(7, result.distance)
        assertEquals(3, result.steps)
        assertEquals(2, result.repeats)
        assertEquals(125.milliseconds, assertIs<Timing.Duration>(result.rate).duration)
        assertNull(result.color)
        assertTrue(result.isMuted)
    }
}
