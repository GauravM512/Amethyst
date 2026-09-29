package dev.anthonyhfm.amethyst.workspace

import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.kaskobi.AutoPageAdapter
import dev.anthonyhfm.amethyst.core.engine.elements.Chain
import dev.anthonyhfm.amethyst.core.util.Timing
import dev.anthonyhfm.amethyst.devices.effects.delay.DelayChainDevice
import dev.anthonyhfm.amethyst.devices.effects.delay.DelayChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDevice
import dev.anthonyhfm.amethyst.devices.effects.switch.MacroControlChainDeviceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds

class AutoPageTimingTest {
    @Test
    fun synchronizedAutoPageUsesBeatDelayAndLeavesEnoughPlaybackTail() {
        val originalBpm = AbletonConverter.bpm

        try {
            AbletonConverter.bpm = 150.0
            val states = AutoPageAdapter(
                blob = """{"Release Trigger":[1],"live.numbox":[12],"live.numbox[43]":[7],"live.numbox[44]":[25],"live.text[39]":[1]}"""
            ).toDeviceStates()

            val delay = assertIs<DelayChainDeviceState>(states[0])
            assertEquals(100L, (delay.timing as Timing.Duration).duration.inWholeMilliseconds)
            assertEquals(11, assertIs<MacroControlChainDeviceState>(states[1]).value)

            val delayDevice = DelayChainDevice().apply {
                state.value = delay
            }
            val chain = Chain().apply {
                add(device = delayDevice, fromUser = false)
            }
            assertEquals(100L, autoPlayDelayTailMs(chains = listOf(chain), bpm = 150.0))
        } finally {
            AbletonConverter.bpm = originalBpm
        }
    }

    @Test
    fun timedAutoPageUsesMillisecondValue() {
        val states = AutoPageAdapter(
            blob = """{"live.numbox":[3],"live.numbox[43]":[7],"live.numbox[44]":[25],"live.text[39]":[0]}"""
        ).toDeviceStates()

        val delay = assertIs<DelayChainDeviceState>(states[0])
        assertEquals(25.milliseconds, (delay.timing as Timing.Duration).duration)
        assertEquals(2, assertIs<MacroControlChainDeviceState>(states[1]).value)
    }

    @Test
    fun zeroDelayDoesNotInsertDelayDevice() {
        val states = AutoPageAdapter(
            blob = """{"live.numbox":[2],"live.numbox[43]":[0],"live.numbox[44]":[25],"live.text[39]":[1]}"""
        ).toDeviceStates()

        assertEquals(1, states.size)
        assertEquals(1, assertIs<MacroControlChainDeviceState>(states[0]).value)
        assertEquals(0L, autoPlayDelayTailMs(chains = emptyList(), bpm = 120.0))
    }

    @Test
    fun autoPlayTailFollowsLongestNestedDelayPath() {
        fun delay(milliseconds: Long): DelayChainDevice = DelayChainDevice().apply {
            state.value = DelayChainDeviceState(
                timing = Timing.Duration(milliseconds.milliseconds),
            )
        }

        val group = GroupChainDevice()
        group.state.value.groups.first().chain.addAll(
            devicesToAdd = listOf(delay(100), delay(100)),
            fromUser = false,
        )
        group.createGroup()
        group.state.value.groups.last().chain.add(
            device = delay(250),
            fromUser = false,
        )
        val sampling = Chain().apply {
            add(device = group, fromUser = false)
            add(device = delay(50), fromUser = false)
        }
        val lights = Chain().apply {
            add(device = delay(120), fromUser = false)
        }

        assertEquals(300L, autoPlayDelayTailMs(chains = listOf(sampling, lights), bpm = 150.0))
    }
}
