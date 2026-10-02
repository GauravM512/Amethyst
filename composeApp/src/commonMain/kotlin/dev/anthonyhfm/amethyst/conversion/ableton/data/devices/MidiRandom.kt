package dev.anthonyhfm.amethyst.conversion.ableton.data.devices

import dev.anthonyhfm.amethyst.conversion.ableton.data.AbletonDevice
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement

@Serializable
data class MidiRandom(
    @SerialName("Id")
    val id: Int,

    @XmlElement
    val on: AbletonOn = AbletonOn(),

    val chance: Chance,
    val choices: Choices,
    val scale: Scale = Scale(AbletonManual(1.0)),
    val sign: Sign = Sign(AbletonManual(0)),
    val alternate: Alternate
) : AbletonDevice {
    @Serializable
    data class Chance(
        val manual: AbletonManual<Double>
    )

    @Serializable
    data class Choices(
        val manual: AbletonManual<Double>
    )

    @Serializable
    data class Scale(
        val manual: AbletonManual<Double>
    )

    @Serializable
    data class Sign(
        val manual: AbletonManual<Int>
    )

    @Serializable
    data class Alternate(
        val manual: AbletonManual<Boolean>
    )
}
