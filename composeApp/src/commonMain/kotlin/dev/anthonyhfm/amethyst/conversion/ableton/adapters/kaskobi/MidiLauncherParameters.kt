package dev.anthonyhfm.amethyst.conversion.ableton.adapters.kaskobi

import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxDevice
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxParameter

internal class MidiLauncherParameters(
    device: MxDevice,
    private val hash: String
) {
    private val parameters = device.parameterList.parameterList.parameters

    fun value(
        name: String,
        indicesByHash: Map<String, Int> = emptyMap()
    ): Double? {
        val parameter = parameters.firstOrNull {
            it.name?.value?.equals(other = name, ignoreCase = true) == true
        } ?: indicesByHash[hash]?.let { index ->
            parameters.firstOrNull { it.index == index }
        }

        return when (parameter) {
            is MxParameter.MxDIntParameter -> parameter.timeable.manual.value.toDouble()
            is MxParameter.MxDEnumParameter -> parameter.timeable.manual.value.toDouble()
            is MxParameter.MxDFloatParameter -> parameter.timeable.manual.value.toDouble()
            null -> null
        }
    }
}
