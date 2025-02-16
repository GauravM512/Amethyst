package dev.anthonyhfm.amethyst.devices.audio

import androidx.compose.runtime.Composable
import dev.anthonyhfm.amethyst.core.midi.data.MidiEffectData
import dev.anthonyhfm.amethyst.devices.BaseDevice

abstract class AudioDevice : BaseDevice<MidiEffectData> {
    @Composable
    abstract override fun Content()
}