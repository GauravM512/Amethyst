package dev.anthonyhfm.amethyst.conversion.apollo.adapters

import dev.anthonyhfm.amethyst.conversion.apollo.data.ApolloAdapter
import dev.anthonyhfm.amethyst.conversion.apollo.data.ApolloModel
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects.color_filter.ColorFilterChainDeviceState

class ApolloColorFilterAdapter(
    model: ApolloModel.Device.ColorFilter
) : ApolloAdapter<ApolloModel.Device.ColorFilter>(model) {
    override fun toDeviceState(): DeviceState {
        return ColorFilterChainDeviceState(
            hue = model.hue.toInt(),
            hueTolerance = model.hueTolerance.toFloat(),
            saturation = model.saturation.toFloat(),
            saturationTolerance = model.saturationTolerance.toFloat(),
            value = model.value.toFloat(),
            valueTolerance = model.valueTolerance.toFloat()
        )
    }
}
