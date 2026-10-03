package dev.anthonyhfm.amethyst.devices.ableton

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.devices.ChainDeviceFactory
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.GenericChainDevice
import dev.anthonyhfm.amethyst.devices.TimelineDuration
import dev.anthonyhfm.amethyst.devices.TimelineDurationContext
import dev.anthonyhfm.amethyst.ui.components.primitives.ChainDeviceShell
import dev.anthonyhfm.amethyst.workspace.chain.ui.LocalTitleBarModifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

class AbletonPitchRangeChainDevice : GenericChainDevice<AbletonPitchRangeChainDeviceState>() {
    override val state = MutableStateFlow(AbletonPitchRangeChainDeviceState())

    override fun timelineDuration(context: TimelineDurationContext): TimelineDuration = TimelineDuration.None

    @Composable
    override fun Content() {
        val selections by SelectionManager.selections.collectAsState()

        ChainDeviceShell(
            title = "Ableton Key Zone",
            isSelected = selections.any { it.selectionUUID == selectionUUID },
            isDragging = isDragging.value,
            modifier = Modifier
                .width(width = 150.dp),
            titleBarModifier = LocalTitleBarModifier.current,
        ) {
            AbletonCompatibilityNotice()
        }
    }

    override fun signalEnter(n: List<Signal>) {
        val current = state.value
        val filtered = n.filter { signal ->
            if (signal is Signal.AudioSignal) {
                true
            } else {
                val pitch = AbletonNoteSpace.note(signal)?.pitch
                pitch != null && pitch in current.minimum..current.maximum
            }
        }

        if (filtered.isNotEmpty()) {
            signalExit?.invoke(filtered)
        }
    }

    companion object : ChainDeviceFactory<AbletonPitchRangeChainDeviceState> {
        override val stateClass = AbletonPitchRangeChainDeviceState::class
        override val serializer = AbletonPitchRangeChainDeviceState.serializer()
        override fun create() = AbletonPitchRangeChainDevice()
    }
}

@Serializable
data class AbletonPitchRangeChainDeviceState(
    val minimum: Int = 0,
    val maximum: Int = 127,
) : DeviceState()
