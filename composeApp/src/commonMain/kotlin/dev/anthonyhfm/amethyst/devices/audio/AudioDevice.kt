package dev.anthonyhfm.amethyst.devices.audio

import androidx.compose.runtime.Composable
import dev.anthonyhfm.amethyst.core.midi.data.MidiEffectData
import dev.anthonyhfm.amethyst.devices.BaseDevice
import dev.anthonyhfm.amethyst.devices.DeviceState
import kotlinx.coroutines.flow.MutableStateFlow

abstract class AudioDevice <State : DeviceState> : BaseDevice<MidiEffectData, State> {
    abstract override val state: MutableStateFlow<State>

    @Composable
    abstract override fun Content()
}