package dev.anthonyhfm.amethyst.core.midi.data

import dev.anthonyhfm.amethyst.core.midi.devices.LaunchpadDevice
import dev.atsushieno.ktmidi.MidiInput
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ProjectDeviceConfig(
    @Transient
    val input: MidiInput? = null,
    @Transient
    val launchpadDevice: LaunchpadDevice? = null
)