package dev.anthonyhfm.amethyst.conversion.ableton.data.devices

import dev.anthonyhfm.amethyst.conversion.ableton.data.AbletonDevice
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonKeyMidi
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonMidiControllerRange
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement

@Serializable
data class MidiVelocity(
    @SerialName("Id")
    val id: Int,

    @XmlElement
    val on: AbletonOn = AbletonOn(),

    val maxOut: MaxOut,
    val minOut: MinOut? = null,
    val mode: Mode? = null,
    val lowest: Lowest? = null,
    val range: Range? = null,
    val random: Random? = null,
) : AbletonDevice {
    @Serializable
    data class MaxOut(
        val manual: AbletonManual<Int>,
        @XmlElement
        val keyMidi: AbletonKeyMidi? = null,
        @XmlElement
        val midiControllerRange: AbletonMidiControllerRange? = null,
    )

    @Serializable
    data class MinOut(
        val manual: AbletonManual<Int>,
        @XmlElement
        val keyMidi: AbletonKeyMidi? = null,
        @XmlElement
        val midiControllerRange: AbletonMidiControllerRange? = null,
    )

    @Serializable
    data class Mode(
        val manual: AbletonManual<Int>,
    )

    @Serializable
    data class Lowest(
        val manual: AbletonManual<Int>,
    )

    @Serializable
    data class Range(
        val manual: AbletonManual<Int>,
    )

    @Serializable
    data class Random(
        val manual: AbletonManual<Int>,
    )
}
