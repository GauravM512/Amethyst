package dev.anthonyhfm.amethyst.devices.ableton

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.util.Timing
import dev.anthonyhfm.amethyst.devices.TimelineDuration
import dev.anthonyhfm.amethyst.devices.TimelineDurationContext
import dev.anthonyhfm.amethyst.ui.components.asTiming
import dev.anthonyhfm.amethyst.ui.components.toExactMsValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class AbletonMidiRoutingTest {
    @Test
    fun rhythmRatesUseExactBpmIntervalsWithoutStepRounding() {
        val introRate = Timing.Rythm(Timing.Rythm.RythmTiming._1_48)
        val dropRate = Timing.Rythm(Timing.Rythm.RythmTiming._1_96)

        assertEquals(introRate, "1/48".asTiming())
        assertEquals(55.555555, introRate.toExactMsValue(bpm = 90.0), absoluteTolerance = 0.00001)
        assertEquals(27.777777, dropRate.toExactMsValue(bpm = 90.0), absoluteTolerance = 0.00001)
        assertEquals(12_000.0, introRate.toExactMsValue(bpm = 90.0) * 216, absoluteTolerance = 0.001)
    }

    @Test
    fun arpeggiatorDurationIncludesOriginalStepDistanceAndRepeats() {
        val device = AbletonArpeggiatorChainDevice().apply {
            state.value = AbletonArpeggiatorChainDeviceState(
                rate = Timing.Duration(100.milliseconds),
                distance = -4,
                steps = 2,
                repeats = 2,
                gate = 50f,
            )
        }

        assertEquals(
            TimelineDuration.Finite(milliseconds = 550),
            device.timelineDuration(TimelineDurationContext(bpm = 120.0)),
        )
    }

    @Test
    fun keyZoneFiltersOriginalPitchBeforeLaterTransposition() {
        val source = Signal.LED(origin = null, x = -1, y = -1, color = Color.Green)
        val note = AbletonNoteSpace.Note(pitch = 0, targetX = 10, targetY = 0)
        val input = AbletonNoteSpace.withPitch(source, note, 0) as Signal.LED
        val zone = AbletonPitchRangeChainDevice().apply {
            state.value = AbletonPitchRangeChainDeviceState(minimum = 0, maximum = 0)
        }
        val pitcher = AbletonPitcherChainDevice().apply {
            state.value = AbletonPitcherChainDeviceState(pitch = 36)
        }
        val output = mutableListOf<Signal>()
        zone.signalExit = pitcher::signalEnter
        pitcher.signalExit = { signals -> output.addAll(signals) }

        zone.signalEnter(listOf(input))

        val transformed = output.single() as Signal.LED
        assertEquals(36, transformed.extras[AbletonNoteSpace.PITCH])
        assertEquals(11, AbletonNoteSpace.project(transformed)?.x)
    }

    @Test
    fun alternateRandomCyclesChoicesAndKeepsNoteOffOnTheChosenPitch() {
        val device = AbletonRandomChainDevice().apply {
            state.value = AbletonRandomChainDeviceState(
                chance = 1.0,
                choices = 2,
                scale = 2,
                alternate = true,
            )
        }
        val input = AbletonNoteSpace.withPitch(
            signal = Signal.Midi(origin = null, x = 1, y = 8, velocity = 127),
            note = AbletonNoteSpace.Note(pitch = 36, targetX = 0, targetY = 0),
            pitch = 36,
        ) as Signal.Midi
        val output = mutableListOf<Signal>()
        device.signalExit = { signals -> output.addAll(signals) }

        repeat(3) {
            device.signalEnter(listOf(input))
            device.signalEnter(listOf(input.copy(velocity = 0)))
        }

        assertEquals(listOf(36, 36, 38, 38, 36, 36), output.map {
            it.extras[AbletonNoteSpace.PITCH]
        })
        assertTrue(output.filterIsInstance<Signal.Midi>().filterIndexed { index, _ -> index % 2 == 1 }
            .all { it.velocity == 0 })
    }
}
