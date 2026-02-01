package dev.anthonyhfm.amethyst.devices.effects.transmit

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.LEDChainDevice
import dev.anthonyhfm.amethyst.ui.components.AmethystDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

class TransmitChainDevice : LEDChainDevice<TransmitChainDeviceState>() {
    private val MAX_CHANNELS = 16

    override val state = MutableStateFlow(TransmitChainDeviceState())

    @Composable
    override fun Content() {
        val selections by SelectionManager.selections.collectAsState()

        AmethystDevice(
            title = "Transmit",
            isSelected = selections.any { it.selectionUUID == this.selectionUUID },
            isDragging = isDragging.value,
            modifier = Modifier.width(140.dp)
        ) {

        }
    }

    override fun ledSignalEnter(n: List<Signal.LED>) {

    }

    companion object {
        val receivers: MutableList<TransmitChainDevice> = mutableListOf()
    }
}

@Serializable
data class TransmitChainDeviceState(
    val mode: Mode = Mode.Send,
    val channel: Int = 0
) : DeviceState() {
    enum class Mode {
        Send,
        Receive
    }
}