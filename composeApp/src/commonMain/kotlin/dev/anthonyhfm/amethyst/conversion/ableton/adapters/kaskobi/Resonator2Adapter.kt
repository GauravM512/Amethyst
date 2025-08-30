package dev.anthonyhfm.amethyst.conversion.ableton.adapters.kaskobi

import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.utils.XmlElement
import dev.anthonyhfm.amethyst.devices.DeviceState

class Resonator2Adapter(
    val blob: ByteArray,
    val xml: XmlElement
) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> {
        val parameterList = xml.querySelector("ParameterList")[1]

        val timeBetweenColorsXml = parameterList
            .querySelector("MxDFloatParameter").find {
                it.attributes["Id"] == "21"
            }

        /*val timeBetweenColorsXml = parameterList
            .querySelector("MxDFloatParameter").find {
                it.attributes["Id"] == "21"
            }*/

        return emptyList()
    }
}