package dev.anthonyhfm.amethyst.core.data.tracks

import dev.anthonyhfm.amethyst.core.midi.data.MidiInputData
import dev.anthonyhfm.amethyst.devices.audio.AudioDevice

class AudioTrack(
    override val name: String,
    override var projectDeviceIndex: Int? = null
) : Track<AudioDevice<*>>() {
    override fun processMidiInputData(midiInputData: MidiInputData) {

    }
}