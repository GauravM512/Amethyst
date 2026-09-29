package dev.anthonyhfm.amethyst.devices.ableton

import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.util.Timing
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class AbletonArpeggiatorHoldTest {
    @Test
    fun shortPressContinuesFiniteTransposedPatternAndReleasesEachNote() {
        val device = AbletonArpeggiatorChainDevice().apply {
            state.value = AbletonArpeggiatorChainDeviceState(
                rate = Timing.Duration(40.milliseconds),
                distance = 4,
                steps = 3,
                repeats = 1,
                hold = true,
                gate = 50f,
            )
        }
        val output = CopyOnWriteArrayList<Signal.Midi>()
        device.signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.Midi>()) }
        val input = AbletonNoteSpace.withPitch(
            signal = Signal.Midi(origin = null, x = 4, y = 4, velocity = 127),
            note = AbletonNoteSpace.Note(pitch = 55, targetX = 0, targetY = 0),
            pitch = 55,
        ) as Signal.Midi

        device.signalEnter(listOf(input))
        Thread.sleep(10)
        device.signalEnter(listOf(input.copy(velocity = 0)))
        Thread.sleep(250)

        val onPitches = output.filter { it.velocity > 0 }.mapNotNull { it.extras[AbletonNoteSpace.PITCH] }
        val offPitches = output.filter { it.velocity == 0 }.mapNotNull { it.extras[AbletonNoteSpace.PITCH] }
        assertEquals(listOf(55, 59, 63, 67), onPitches)
        assertEquals(onPitches.sorted(), offPitches.sorted())
    }
}
