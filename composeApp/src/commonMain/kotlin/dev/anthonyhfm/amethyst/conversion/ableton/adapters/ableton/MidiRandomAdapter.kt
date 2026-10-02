package dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MidiRandom
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.ableton.AbletonRandomChainDeviceState

class MidiRandomAdapter(
    private val device: MidiRandom,
) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> = listOf(
        AbletonRandomChainDeviceState(
            chance = device.chance.manual.value,
            choices = device.choices.manual.value.toInt(),
            scale = device.scale.manual.value.toInt(),
            sign = device.sign.manual.value,
            alternate = device.alternate.manual.value,
        )
    ).withMuteState(device.on.manual.value)
}
