package dev.anthonyhfm.amethyst.devices.ableton

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.util.Timing
import dev.anthonyhfm.amethyst.devices.effects.hold.HoldChainDevice
import dev.anthonyhfm.amethyst.devices.effects.hold.HoldChainDeviceState
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class AbletonNoteLengthReleaseTest {
    @Test
    fun unmatchedReleaseDoesNotCreateAWhiteNote() {
        val device = HoldChainDevice().apply {
            state.value = HoldChainDeviceState(
                timing = Timing.Duration(30.milliseconds),
                gate = 0.5f,
                onRelease = true,
            )
        }
        val output = CopyOnWriteArrayList<Signal.LED>()
        device.signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.LED>()) }
        device.signalEnter(listOf(Signal.LED(origin = null, x = 3, y = 3, color = Color.Black)))
        Thread.sleep(30)

        assertEquals(0, output.size)
    }

    @Test
    fun releaseKeepsIncomingVelocityAndSeparatesHiddenPitches() {
        val device = HoldChainDevice().apply {
            state.value = HoldChainDeviceState(
                timing = Timing.Duration(30.milliseconds),
                gate = 0.5f,
                onRelease = true,
            )
        }
        val output = CopyOnWriteArrayList<Signal.Midi>()
        device.signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.Midi>()) }

        val inputs = listOf(0, 1).mapIndexed { index, pitch ->
            AbletonNoteSpace.withPitch(
                signal = Signal.Midi(origin = null, x = 3, y = 3, velocity = 40 + index * 20),
                note = AbletonNoteSpace.Note(pitch = pitch, targetX = 0, targetY = 0),
                pitch = pitch,
            ) as Signal.Midi
        }

        device.signalEnter(inputs)
        device.signalEnter(inputs.map { it.copy(velocity = 0) })
        Thread.sleep(100)

        assertEquals(
            expected = setOf(0 to 40, 1 to 60),
            actual = output.filter { it.velocity > 0 }
                .map { it.extras[AbletonNoteSpace.PITCH] to it.velocity }
                .toSet(),
        )
        assertEquals(
            expected = setOf(0, 1),
            actual = output.filter { it.velocity == 0 }
                .mapNotNull { it.extras[AbletonNoteSpace.PITCH] }
                .toSet(),
        )
    }

    @Test
    fun releaseKeepsIncomingLedColor() {
        val device = HoldChainDevice().apply {
            state.value = HoldChainDeviceState(
                timing = Timing.Duration(30.milliseconds),
                gate = 0.5f,
                onRelease = true,
            )
        }
        val output = CopyOnWriteArrayList<Signal.LED>()
        device.signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.LED>()) }
        val input = AbletonNoteSpace.withPitch(
            signal = Signal.LED(origin = null, x = 3, y = 3, color = Color.Red),
            note = AbletonNoteSpace.Note(pitch = 0, targetX = 0, targetY = 0),
            pitch = 0,
        ) as Signal.LED

        device.signalEnter(listOf(input))
        device.signalEnter(listOf(input.copy(color = Color.Black)))
        Thread.sleep(100)

        assertEquals(Color.Red, output.first().color)
        assertEquals(Color.Black, output.last().color)
    }
}
