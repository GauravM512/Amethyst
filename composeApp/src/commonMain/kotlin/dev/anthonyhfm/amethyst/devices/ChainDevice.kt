package dev.anthonyhfm.amethyst.devices

import androidx.compose.runtime.Composable
import dev.anthonyhfm.amethyst.core.heaven.elements.Signal
import dev.anthonyhfm.amethyst.core.heaven.elements.SignalReceiver
import dev.anthonyhfm.amethyst.core.util.UUID
import dev.anthonyhfm.amethyst.core.util.randomUUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

abstract class ChainDevice <State : @Serializable DeviceState> : SignalReceiver() {
    abstract val state: MutableStateFlow<State>

    /**
     * A unique identifier for the device, used to differentiate between devices in the UI
     */
    internal val internalUUID: String = UUID.randomUUID()

    @Composable
    abstract fun Content()

    abstract override fun midiEnter(n: List<Signal>)
}