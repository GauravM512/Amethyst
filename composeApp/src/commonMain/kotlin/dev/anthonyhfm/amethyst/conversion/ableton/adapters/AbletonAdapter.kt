package dev.anthonyhfm.amethyst.conversion.ableton.adapters

import dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton.MidiEffectGroupAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.utils.XmlElement
import dev.anthonyhfm.amethyst.devices.DeviceState

abstract class AbletonAdapter {
    abstract fun toDeviceStates(): List<DeviceState>

    companion object {
        fun resolveAdapter(xml: XmlElement): AbletonAdapter? {
            return when (xml.name) {
                "MidiEffectGroupDevice" -> MidiEffectGroupAdapter(xml)

                else -> {
                    println("Unsupported Ableton XML element: ${xml.name}")
                    null
                }
            }
        }
    }
}