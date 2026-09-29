package dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MidiPitcher
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.ableton.AbletonPitcherChainDeviceState

class MidiPitcherAdapter(
    private val device: MidiPitcher,
    private val rackMacroValues: List<Float>? = null,
) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> {
        return listOf(
            AbletonPitcherChainDeviceState(
                pitch = AbletonMacroMapping.effectiveInt(
                    manualValue = device.pitch.manual.value,
                    keyMidi = device.pitch.keyMidi,
                    controllerRange = device.pitch.midiControllerRange,
                    parentMacroValues = rackMacroValues,
                )
            )
        ).withMuteState(device.on.manual.value)
    }
}
