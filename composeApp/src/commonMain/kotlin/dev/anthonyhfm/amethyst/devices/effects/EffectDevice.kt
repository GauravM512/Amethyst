package dev.anthonyhfm.amethyst.devices.effects

import androidx.compose.runtime.Composable
import dev.anthonyhfm.amethyst.core.midi.data.MidiEffectData
import dev.anthonyhfm.amethyst.devices.BaseDevice
import dev.anthonyhfm.amethyst.devices.DeviceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

abstract class EffectDevice <State : DeviceState> : BaseDevice<MidiEffectData, State> {
    abstract override val state: MutableStateFlow<State>

    var midiOutput: (MidiEffectData) -> Unit = { }

    @Composable
    abstract override fun Content()
}