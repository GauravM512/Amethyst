package dev.anthonyhfm.amethyst.devices.effects.choke

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.elements.Chain
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.devices.Chokeable
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.GenericChainDevice
import dev.anthonyhfm.amethyst.devices.TimelineDuration
import dev.anthonyhfm.amethyst.devices.TimelineDurationContext
import dev.anthonyhfm.amethyst.devices.ableton.AbletonNoteSpace
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class ChokeChainDeviceTest {
    @Test
    fun startSendsOnPressAndLegacyStillSendsOnRelease() {
        val receiver = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 31)
        }
        val probe = ChokeProbeDevice()
        receiver.state.value.chain.add(device = probe, fromUser = false)
        val sender = ChokeChainDevice().apply {
            state.value = state.value.copy(
                target = 31,
                mode = ChokeChainDeviceState.ChokeMode.Start,
            )
        }
        val press = Signal.Midi(origin = null, x = 1, y = 1, velocity = 127)

        try {
            sender.signalEnter(n = listOf(press))
            assertEquals(1, probe.chokeCount)
            sender.signalEnter(n = listOf(press.copy(velocity = 0)))
            assertEquals(1, probe.chokeCount)
            sender.state.value = sender.state.value.copy(mode = ChokeChainDeviceState.ChokeMode.Legacy)
            sender.signalEnter(n = listOf(press.copy(velocity = 0)))
            assertEquals(2, probe.chokeCount)
        } finally {
            sender.onRemovedFromChain()
            receiver.onRemovedFromChain()
        }
    }

    @Test
    fun receiveDoesNotSendAndNoteZeroSendsOnlyForOutputNoteOnZero() {
        val receiver = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 30)
        }
        val probe = ChokeProbeDevice()
        receiver.state.value.chain.add(device = probe, fromUser = false)
        val sender = ChokeChainDevice().apply {
            state.value = state.value.copy(
                target = 30,
                mode = ChokeChainDeviceState.ChokeMode.Receive,
            )
        }
        val press = Signal.Midi(origin = null, x = 1, y = 1, velocity = 127)

        try {
            sender.signalEnter(n = listOf(press))
            assertEquals(0, probe.chokeCount)
            sender.state.value = sender.state.value.copy(mode = ChokeChainDeviceState.ChokeMode.NoteZero)
            sender.state.value.chain.signalExit?.invoke(listOf(press))
            assertEquals(0, probe.chokeCount)
            val zero = press.copy(extras = mapOf(AbletonNoteSpace.PITCH to 0, "ableton.targetX" to 0, "ableton.targetY" to 0))
            sender.state.value.chain.signalExit?.invoke(listOf(zero.copy(velocity = 0)))
            assertEquals(0, probe.chokeCount)
            sender.state.value.chain.signalExit?.invoke(listOf(zero))
            assertEquals(1, probe.chokeCount)
        } finally {
            sender.onRemovedFromChain()
            receiver.onRemovedFromChain()
        }
    }

    @Test
    fun endBindsRestoredKeyframesCompletionWithoutSendingOnInputOrSilence() {
        val receiver = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 29)
        }
        val probe = ChokeProbeDevice()
        receiver.state.value.chain.add(device = probe, fromUser = false)
        val keyframes = KeyframesChainDevice()
        val sender = ChokeChainDevice().apply {
            state.value = state.value.copy(
                target = 29,
                mode = ChokeChainDeviceState.ChokeMode.End,
            )
            state.value.chain.add(device = keyframes, fromUser = false)
            onStateRestored()
        }

        try {
            sender.signalEnter(n = listOf(Signal.Midi(origin = null, x = 1, y = 1, velocity = 127)))
            sender.state.value.chain.signalExit?.invoke(emptyList())
            assertEquals(0, probe.chokeCount)
            keyframes.onPlaybackEnd?.invoke()
            assertEquals(1, probe.chokeCount)
        } finally {
            sender.onRemovedFromChain()
            receiver.onRemovedFromChain()
        }
    }

    @Test
    fun repeatedTriggerChokesItsOwnPreviousOutputAndEffect() {
        val output = mutableListOf<Signal.LED>()
        val effect = ChokeProbeDevice()
        val choke = ChokeChainDevice().apply {
            state.value.chain.add(device = effect, fromUser = false)
            signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.LED>()) }
        }

        try {
            val lit = Signal.LED(origin = "pad", x = 1, y = 2, color = Color.Red)
            choke.signalEnter(listOf(lit))
            val firstChokeCount = effect.chokeCount

            choke.signalEnter(listOf(lit))

            assertEquals(listOf(lit, lit.copy(color = Color.Black), lit), output)
            assertEquals(firstChokeCount + 1, effect.chokeCount)
        } finally {
            choke.onRemovedFromChain()
        }
    }

    @Test
    fun releaseDoesNotChokeItsOwnEffect() {
        val output = mutableListOf<Signal.LED>()
        val effect = ChokeProbeDevice()
        val choke = ChokeChainDevice().apply {
            state.value.chain.add(device = effect, fromUser = false)
            signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.LED>()) }
        }

        try {
            val lit = Signal.LED(origin = "pad", x = 1, y = 2, color = Color.Red)
            choke.signalEnter(listOf(lit))
            val firstChokeCount = effect.chokeCount
            output.clear()

            choke.signalEnter(listOf(lit.copy(color = Color.Black)))

            assertEquals(listOf(lit.copy(color = Color.Black)), output)
            assertEquals(firstChokeCount, effect.chokeCount)
        } finally {
            choke.onRemovedFromChain()
        }
    }

    @Test
    fun defaultChannelChokesActiveLedOutput() {
        val output = mutableListOf<Signal.LED>()
        val first = ChokeChainDevice().apply {
            signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.LED>()) }
        }
        val second = ChokeChainDevice()

        try {
            val lit = Signal.LED(origin = "pad", x = 1, y = 2, color = Color.Red)
            first.signalEnter(listOf(lit))
            output.clear()

            second.signalEnter(listOf(Signal.Midi(origin = "trigger", x = 0, y = 0, velocity = 127)))

            assertEquals(listOf(lit.copy(color = Color.Black)), output)
        } finally {
            first.onRemovedFromChain()
            second.onRemovedFromChain()
        }
    }

    @Test
    fun chokeClearsOnlyActiveLedCoordinatesAndLayers() {
        val output = mutableListOf<Signal.LED>()
        val first = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 1)
            signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.LED>()) }
        }
        val second = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 1)
        }

        try {
            val red = Signal.LED(origin = "pad", x = 2, y = 3, color = Color.Red, layer = 4)
            val blue = Signal.LED(origin = "pad", x = 2, y = 3, color = Color.Blue, layer = 5)
            val released = Signal.LED(origin = "pad", x = 6, y = 7, color = Color.Green, layer = 4)

            first.signalEnter(listOf(red, blue, released))
            first.signalEnter(listOf(released.copy(color = Color.Black)))
            output.clear()

            second.signalEnter(listOf(Signal.Midi(origin = "trigger", x = 0, y = 0, velocity = 127)))

            assertEquals(
                setOf(red.copy(color = Color.Black), blue.copy(color = Color.Black)),
                output.toSet(),
            )

            output.clear()
            second.signalEnter(listOf(Signal.Midi(origin = "trigger", x = 0, y = 0, velocity = 127)))
            assertEquals(emptyList(), output)
        } finally {
            first.onRemovedFromChain()
            second.onRemovedFromChain()
        }
    }

    @Test
    fun restoredTargetIsUsedForChoking() {
        val output = mutableListOf<Signal.LED>()
        val first = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 2)
            signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.LED>()) }
        }
        val second = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 1)
        }

        try {
            val lit = Signal.LED(origin = "pad", x = 1, y = 1, color = Color.Red)
            first.signalEnter(listOf(lit))
            output.clear()

            second.signalEnter(listOf(Signal.Midi(origin = "trigger", x = 0, y = 0, velocity = 127)))
            assertEquals(emptyList(), output)

            second.state.value = second.state.value.copy(target = 2)
            second.signalEnter(listOf(Signal.Midi(origin = "trigger", x = 0, y = 0, velocity = 127)))
            assertEquals(listOf(lit.copy(color = Color.Black)), output)
        } finally {
            first.onRemovedFromChain()
            second.onRemovedFromChain()
        }
    }

    @Test
    fun restoredChainOutputIsTracked() {
        val output = mutableListOf<Signal.LED>()
        val first = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 3, chain = Chain())
            onStateRestored()
            signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.LED>()) }
        }
        val second = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 3)
        }

        try {
            val lit = Signal.LED(origin = "pad", x = 4, y = 5, color = Color.Green)
            first.signalEnter(listOf(lit))
            output.clear()

            second.signalEnter(listOf(Signal.Midi(origin = "trigger", x = 0, y = 0, velocity = 127)))

            assertEquals(listOf(lit.copy(color = Color.Black)), output)
        } finally {
            first.onRemovedFromChain()
            second.onRemovedFromChain()
        }
    }

    @Test
    fun repeatedChildOutputsAndReleasesKeepOnlyActiveOutputs() {
        val output = mutableListOf<Signal.LED>()
        val first = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 4)
            signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.LED>()) }
        }
        val second = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 4)
        }

        try {
            val lit = List(300) { index ->
                Signal.LED(
                    origin = "pad",
                    x = index,
                    y = index % 7,
                    color = Color.Red,
                    layer = index % 5,
                )
            }
            first.state.value.chain.signalExit?.invoke(lit)
            first.state.value.chain.signalExit?.invoke(lit.map { it.copy(color = Color.Blue) })
            val released = lit.filterIndexed { index, _ -> index % 2 == 0 }
            first.state.value.chain.signalExit?.invoke(released.map { it.copy(color = Color.Black) })
            first.state.value.chain.signalExit?.invoke(released.map { it.copy(color = Color.Green) })
            first.state.value.chain.signalExit?.invoke(released.map { it.copy(color = Color.Black) })
            output.clear()

            second.signalEnter(listOf(Signal.Midi(origin = "trigger", x = 0, y = 0, velocity = 127)))

            assertEquals(
                lit.filterIndexed { index, _ -> index % 2 != 0 }.map { it.copy(color = Color.Black) }.toSet(),
                output.toSet(),
            )
        } finally {
            first.onRemovedFromChain()
            second.onRemovedFromChain()
        }
    }

    @Serializable
    private class ChokeProbeState : DeviceState()

    private class ChokeProbeDevice : GenericChainDevice<ChokeProbeState>(), Chokeable {
        override val state = MutableStateFlow(ChokeProbeState())
        var chokeCount = 0

        @Composable
        override fun Content() = Unit

        override fun timelineDuration(context: TimelineDurationContext): TimelineDuration = TimelineDuration.None

        override fun signalEnter(n: List<Signal>) {
            signalExit?.invoke(n)
        }

        override fun onChoke() {
            chokeCount++
        }
    }
}
