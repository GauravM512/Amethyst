package dev.anthonyhfm.amethyst.devices.effects.choke

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.engine.elements.Chain
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.GenericChainDevice
import dev.anthonyhfm.amethyst.ui.components.AmethystDevice
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

class ChokeChainDevice : GenericChainDevice<ChokeChainDeviceState>() {
    override val state = MutableStateFlow(ChokeChainDeviceState())

    @Composable
    override fun Content() {
        val selections by SelectionManager.selections.collectAsState()

        AmethystDevice(
            title = "Choke",
            isSelected = selections.any { it.selectionUUID == this.selectionUUID },
            isDragging = isDragging.value,
            modifier = Modifier.width(140.dp)
        ) {

        }
    }

    override fun signalEnter(n: List<Signal>) {
        signalExit?.invoke(n)
    }
}

@Serializable
data class ChokeChainDeviceState(
    @Transient
    val chain: Chain = Chain(),
    var stateChain: StateChain = StateChain()
) : DeviceState()