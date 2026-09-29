package dev.anthonyhfm.amethyst.devices.ableton

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.util.Palettes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbletonVelocityChainDeviceTest {
    @Test
    fun clipMapsIncomingVelocityThroughOutputRange() {
        val device = AbletonVelocityChainDevice().apply {
            state.value = AbletonVelocityChainDeviceState(
                outLow = 10,
                outHigh = 110,
                lowest = 1,
                range = 127,
            )
        }
        val output = mutableListOf<Signal>()
        device.signalExit = { output.addAll(it) }
        device.signalEnter(listOf(Signal.Midi(origin = null, x = 1, y = 2, velocity = 64)))

        val result = output.single() as Signal.Midi
        assertEquals(60, result.velocity)
        assertEquals(60, result.extras[AbletonVelocityChainDevice.VELOCITY])
    }

    @Test
    fun ledVelocitySurvivesChainedEffectsAndNoteOff() {
        val first = AbletonVelocityChainDevice().apply {
            state.value = AbletonVelocityChainDeviceState(
                outLow = 25,
                outHigh = 25,
            )
        }
        val second = AbletonVelocityChainDevice().apply {
            state.value = AbletonVelocityChainDeviceState(
                outLow = 1,
                outHigh = 127,
            )
        }
        val output = mutableListOf<Signal.LED>()
        first.signalExit = { second.signalEnter(it) }
        second.signalExit = { output.addAll(it.filterIsInstance<Signal.LED>()) }

        first.signalEnter(listOf(Signal.LED(origin = null, x = 1, y = 2, color = Color.White)))
        first.signalEnter(listOf(Signal.LED(origin = null, x = 1, y = 2, color = Color.Black)))

        assertEquals(25, output.first().extras[AbletonVelocityChainDevice.VELOCITY])
        val expected = Palettes.novation[25]
        assertTrue(kotlin.math.abs(output.first().color.red - expected.first / 63f) < 0.01f)
        assertTrue(kotlin.math.abs(output.first().color.green - expected.second / 63f) < 0.01f)
        assertEquals(Color.Black, output.last().color)
    }

    @Test
    fun fixedModeUsesHighOutputAndGateRejectsOutsideRange() {
        val fixed = AbletonVelocityChainDevice().apply {
            state.value = AbletonVelocityChainDeviceState(
                outLow = 1,
                outHigh = 37,
                mode = 2,
            )
        }
        val fixedOutput = mutableListOf<Signal.Midi>()
        fixed.signalExit = { fixedOutput.addAll(it.filterIsInstance<Signal.Midi>()) }
        fixed.signalEnter(listOf(Signal.Midi(origin = null, x = 1, y = 2, velocity = 10)))
        assertEquals(37, fixedOutput.single().velocity)

        val gate = AbletonVelocityChainDevice().apply {
            state.value = AbletonVelocityChainDeviceState(
                outLow = 1,
                outHigh = 127,
                lowest = 40,
                range = 20,
                mode = 1,
            )
        }
        val gateOutput = mutableListOf<Signal.Midi>()
        gate.signalExit = { gateOutput.addAll(it.filterIsInstance<Signal.Midi>()) }
        gate.signalEnter(listOf(Signal.Midi(origin = null, x = 1, y = 2, velocity = 20)))
        gate.signalEnter(listOf(Signal.Midi(origin = null, x = 1, y = 2, velocity = 0)))
        assertTrue(gateOutput.isEmpty())
        gate.signalEnter(listOf(Signal.Midi(origin = null, x = 1, y = 2, velocity = 50)))
        gate.signalEnter(listOf(Signal.Midi(origin = null, x = 1, y = 2, velocity = 0)))
        assertEquals(2, gateOutput.size)
    }

    @Test
    fun gateMatchesOverlappingAcceptedAndRejectedNotesInArrivalOrder() {
        val gate = AbletonVelocityChainDevice().apply {
            state.value = AbletonVelocityChainDeviceState(
                outLow = 50,
                outHigh = 50,
                lowest = 40,
                range = 20,
                mode = 1,
            )
        }
        val output = mutableListOf<Signal.Midi>()
        gate.signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.Midi>()) }

        fun send(velocity: Int) {
            gate.signalEnter(
                n = listOf(
                    Signal.Midi(origin = null, x = 1, y = 2, velocity = velocity)
                )
            )
        }

        send(50)
        send(20)
        send(0)
        assertEquals(listOf(50, 0), output.map { it.velocity })
        send(0)
        assertEquals(listOf(50, 0), output.map { it.velocity })

        send(20)
        send(50)
        send(0)
        assertEquals(listOf(50, 0, 50), output.map { it.velocity })
        send(0)
        assertEquals(listOf(50, 0, 50, 0), output.map { it.velocity })
    }

    @Test
    fun gateReleasesEveryAcceptedOverlappingNote() {
        val gate = AbletonVelocityChainDevice().apply {
            state.value = AbletonVelocityChainDeviceState(
                lowest = 40,
                range = 20,
                mode = 1,
            )
        }
        val output = mutableListOf<Signal.LED>()
        gate.signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.LED>()) }
        val on = Signal.LED(
            origin = null,
            x = 1,
            y = 2,
            color = Color.White,
            extras = mapOf(AbletonVelocityChainDevice.VELOCITY to 50),
        )
        val off = on.copy(color = Color.Black)

        gate.signalEnter(n = listOf(on))
        gate.signalEnter(n = listOf(on))
        gate.signalEnter(n = listOf(off))
        gate.signalEnter(n = listOf(off))

        assertEquals(4, output.size)
        assertEquals(2, output.count { it.color == Color.Black })
    }
}
