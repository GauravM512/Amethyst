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
data class MidiArpeggiator(
    @SerialName("Id")
    val id: Int,

    @XmlElement
    val on: AbletonOn = AbletonOn(),

    @XmlElement
    val mode: Mode = Mode(AbletonManual(0)),

    @XmlElement
    val transposeDistance: TransposeDistance,

    @XmlElement
    val transposeSteps: TransposeSteps,

    @XmlElement
    val syncState: SyncState,

    @XmlElement
    val syncRate: SyncedRate,

    @XmlElement
    val repeatCount: RepeatCount,

    @XmlElement
    val freeRate: FreeRate,

    @XmlElement
    val gate: Gate,

    @XmlElement
    val hold: Hold = Hold(AbletonManual(false)),

    @XmlElement
    val velocityEnabled: VelocitySwitch,

    @XmlElement
    val velocityTarget: VelocityTarget,
) : AbletonDevice {
    @Serializable
    data class Mode(
        val manual: AbletonManual<Int>,
    )

    @Serializable
    data class SyncState(
        val manual: AbletonManual<Boolean>,
    )

    @Serializable
    data class SyncedRate(
        val manual: AbletonManual<Int>,
        @XmlElement
        val keyMidi: AbletonKeyMidi? = null,
        @XmlElement
        val midiControllerRange: AbletonMidiControllerRange? = null,
    )

    @Serializable
    data class FreeRate(
        val manual: AbletonManual<Float>,
    )

    @Serializable
    data class RepeatCount(
        val manual: AbletonManual<Int>,
    )

    @Serializable
    data class Gate(
        val manual: AbletonManual<Float>,
    )

    @Serializable
    data class Hold(
        val manual: AbletonManual<Boolean>,
    )

    @Serializable
    data class VelocitySwitch(
        val manual: AbletonManual<Boolean>,
    )

    @Serializable
    data class VelocityTarget(
        val manual: AbletonManual<Float>,
    )

    @Serializable
    data class TransposeDistance(
        val manual: AbletonManual<Int>,
    )

    @Serializable
    data class TransposeSteps(
        val manual: AbletonManual<Int>,
    )
}
