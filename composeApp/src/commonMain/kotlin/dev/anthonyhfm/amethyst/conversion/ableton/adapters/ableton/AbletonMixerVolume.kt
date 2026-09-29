package dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton

import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.audio.effects.StereoGainChainDeviceState
import kotlin.math.log10

internal fun MutableList<DeviceState>.appendMixerVolume(linearVolume: Float) {
    if (linearVolume == 1f) {
        return
    }

    val gainDb = if (!linearVolume.isFinite() || linearVolume <= 0f) {
        -120f
    } else {
        (20.0 * log10(linearVolume.toDouble())).toFloat()
    }

    add(StereoGainChainDeviceState(gainDb = gainDb))
}
