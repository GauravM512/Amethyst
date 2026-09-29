package dev.anthonyhfm.amethyst.devices.ableton

import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.util.Timing
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class AbletonArpeggiatorChordTest {
    @Test
    fun upPlaysChordNotesInPitchOrder() {
        assertChordOrder(mode = 0, expected = listOf(55, 56))
    }

    @Test
    fun downPlaysChordNotesInReversePitchOrder() {
        assertChordOrder(mode = 1, expected = listOf(56, 55))
    }

    @Test
    fun twelveNoteChordUnfoldsAcrossRateSteps() {
        val device = AbletonArpeggiatorChainDevice().apply {
            state.value = AbletonArpeggiatorChainDeviceState(
                rate = Timing.Duration(28.milliseconds),
                mode = 0,
                steps = 0,
                repeats = 1,
                hold = true,
                gate = 100f,
            )
        }
        val output = CopyOnWriteArrayList<Pair<Long, Int>>()
        val released = CopyOnWriteArrayList<Int>()
        val started = System.nanoTime()
        device.signalExit = { signals ->
            signals.filterIsInstance<Signal.Midi>()
                .forEach { signal ->
                    val pitch = signal.extras[AbletonNoteSpace.PITCH]
                    if (pitch != null) {
                        if (signal.velocity > 0) {
                            output.add((System.nanoTime() - started) / 1_000_000 to pitch)
                        } else {
                            released.add(pitch)
                        }
                    }
                }
        }
        val firstChord = AbletonChordChainDevice().apply {
            state.value = AbletonChordChainDeviceState(shifts = listOf(0, 1, 2, 3, 4, 5))
        }
        val secondChord = AbletonChordChainDevice().apply {
            state.value = AbletonChordChainDeviceState(shifts = listOf(0, 6))
        }
        firstChord.signalExit = { signals -> secondChord.signalEnter(signals) }
        secondChord.signalExit = { signals -> device.signalEnter(signals) }

        val input = AbletonNoteSpace.withPitch(
            signal = Signal.Midi(origin = null, x = 4, y = 4, velocity = 127),
            note = AbletonNoteSpace.Note(pitch = 36, targetX = 0, targetY = 0),
            pitch = 36,
        ) as Signal.Midi
        firstChord.signalEnter(listOf(input))
        Thread.sleep(450)

        assertEquals((36..47).toList(), output.map { it.second })
        assertEquals((36..47).toList(), released.sorted())
        assertTrue(output.last().first - output.first().first >= 240)
    }

    private fun assertChordOrder(mode: Int, expected: List<Int>) {
        val device = AbletonArpeggiatorChainDevice().apply {
            state.value = AbletonArpeggiatorChainDeviceState(
                rate = Timing.Duration(40.milliseconds),
                mode = mode,
                steps = 0,
                repeats = 1,
                hold = true,
                gate = 50f,
            )
        }
        val output = CopyOnWriteArrayList<Signal.Midi>()
        device.signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.Midi>()) }
        val chord = AbletonChordChainDevice().apply {
            state.value = AbletonChordChainDeviceState(shifts = listOf(0, 1))
        }
        chord.signalExit = { signals -> device.signalEnter(signals) }

        val input = AbletonNoteSpace.withPitch(
            signal = Signal.Midi(origin = null, x = 4, y = 4, velocity = 127),
            note = AbletonNoteSpace.Note(pitch = 55, targetX = 0, targetY = 0),
            pitch = 55,
        ) as Signal.Midi

        chord.signalEnter(listOf(input))
        Thread.sleep(10)
        chord.signalEnter(listOf(input.copy(velocity = 0)))
        Thread.sleep(150)

        val onPitches = output.filter { it.velocity > 0 }
            .mapNotNull { it.extras[AbletonNoteSpace.PITCH] }
        val offPitches = output.filter { it.velocity == 0 }
            .mapNotNull { it.extras[AbletonNoteSpace.PITCH] }
        assertEquals(expected, onPitches)
        assertEquals(expected.sorted(), offPitches.sorted())
    }
}
