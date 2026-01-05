package dev.anthonyhfm.amethyst.conversion.ableton.data.devices

import dev.anthonyhfm.amethyst.conversion.ableton.data.AbletonDevice
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MidiPitcher(
    @SerialName("Id")
    val id: Int,
    val pitch: Pitch,
) : AbletonDevice {
    @Serializable
    data class Pitch(
        val manual: AbletonManual<Int>
    )
}