package dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MidiVelocity
import dev.anthonyhfm.amethyst.core.util.Palettes
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.ableton.AbletonVelocityChainDeviceState

class MidiVelocityAdapter(
    private val device: MidiVelocity,
    private val rackMacroValues: List<Float>? = null,
) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> {
        val maximum = AbletonMacroMapping.effectiveInt(
            manualValue = device.maxOut.manual.value,
            keyMidi = device.maxOut.keyMidi,
            controllerRange = device.maxOut.midiControllerRange,
            parentMacroValues = rackMacroValues,
        ).coerceIn(0, 127)
        val minimum = device.minOut?.let { minOut ->
            AbletonMacroMapping.effectiveInt(
                manualValue = minOut.manual.value,
                keyMidi = minOut.keyMidi,
                controllerRange = minOut.midiControllerRange,
                parentMacroValues = rackMacroValues,
            )
        }?.coerceIn(0, 127) ?: maximum
        val palette = AbletonConverter.palette.takeUnless { it.contentEquals(Palettes.novation) }
            ?.map { (red, green, blue) -> (red shl 12) or (green shl 6) or blue }

        return listOf(
            AbletonVelocityChainDeviceState(
                outLow = minimum,
                outHigh = maximum,
                lowest = device.lowest?.manual?.value ?: 1,
                range = device.range?.manual?.value ?: 127,
                mode = device.mode?.manual?.value ?: 0,
                random = device.random?.manual?.value ?: 0,
                palette = palette,
            )
        ).withMuteState(device.on.manual.value)
    }
}
