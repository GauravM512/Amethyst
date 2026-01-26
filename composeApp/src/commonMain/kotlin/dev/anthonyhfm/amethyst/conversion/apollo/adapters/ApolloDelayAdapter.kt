package dev.anthonyhfm.amethyst.conversion.apollo.adapters

import dev.anthonyhfm.amethyst.conversion.apollo.ApolloConverter
import dev.anthonyhfm.amethyst.conversion.apollo.data.ApolloAdapter
import dev.anthonyhfm.amethyst.conversion.apollo.data.ApolloModel
import dev.anthonyhfm.amethyst.conversion.apollo.utils.toTiming
import dev.anthonyhfm.amethyst.core.util.Timing
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects.color.ColorChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.coordinate_filter.CoordinateFilterChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.delay.DelayChainDeviceState
import dev.anthonyhfm.amethyst.ui.components.toMsValue
import kotlin.time.Duration.Companion.milliseconds

class ApolloDelayAdapter(
    model: ApolloModel.Device.Delay
) : ApolloAdapter<ApolloModel.Device.Delay>(model) {
    override fun toDeviceState(): DeviceState {
        val timing: Timing = model.time.toTiming()

        return DelayChainDeviceState(
            timing = timing,
            delayMs = timing.toMsValue(ApolloConverter.bpm.toDouble()),
            gate = model.gate.toFloat() / 2f
        )
    }
}