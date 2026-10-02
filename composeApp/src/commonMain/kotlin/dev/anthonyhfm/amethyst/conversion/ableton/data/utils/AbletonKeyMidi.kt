package dev.anthonyhfm.amethyst.conversion.ableton.data.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@SerialName("KeyMidi")
data class AbletonKeyMidi(
    @XmlElement
    @XmlSerialName("Channel")
    val channel: AbletonManual<Int>? = null,

    @XmlElement
    @XmlSerialName("NoteOrController")
    val noteOrController: AbletonManual<Int>? = null,

    @XmlElement
    @XmlSerialName("LowerRangeNote")
    val lowerRangeNote: AbletonManual<Int>? = null,

    @XmlElement
    @XmlSerialName("UpperRangeNote")
    val upperRangeNote: AbletonManual<Int>? = null,
)
