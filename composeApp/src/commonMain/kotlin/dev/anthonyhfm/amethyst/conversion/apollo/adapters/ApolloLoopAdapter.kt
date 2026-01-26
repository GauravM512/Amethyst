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
import dev.anthonyhfm.amethyst.devices.effects.loop.LoopChainDeviceState
import dev.anthonyhfm.amethyst.ui.components.toMsValue
import kotlin.time.Duration.Companion.milliseconds

class ApolloLoopAdapter(
    model: ApolloModel.Device.Loop
) : ApolloAdapter<ApolloModel.Device.Loop>(model) {
    override fun toDeviceState(): DeviceState {
        val timing: Timing = model.time.toTiming()

        return LoopChainDeviceState(
            repeat = model.repeats,
            timing = timing,
            gate = model.gate.toFloat() / 2f,
            onHold = model.hold
        )
    }
}