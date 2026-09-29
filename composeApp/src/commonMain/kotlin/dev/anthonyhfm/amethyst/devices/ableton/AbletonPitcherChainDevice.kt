package dev.anthonyhfm.amethyst.devices.ableton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.devices.ChainDeviceFactory
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.GenericChainDevice
import dev.anthonyhfm.amethyst.devices.TimelineDuration
import dev.anthonyhfm.amethyst.devices.TimelineDurationContext
import dev.anthonyhfm.amethyst.ui.components.primitives.ChainDeviceShell
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.primaryForeground
import dev.anthonyhfm.amethyst.workspace.chain.ui.LocalTitleBarModifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

class AbletonPitcherChainDevice : GenericChainDevice<AbletonPitcherChainDeviceState>() {
    override fun timelineDuration(context: TimelineDurationContext) = TimelineDuration.None

    override val state = MutableStateFlow(AbletonPitcherChainDeviceState())

    @Composable
    override fun Content() {
        val selections by SelectionManager.selections.collectAsState()
        val isSelected = selections.any { it.selectionUUID == selectionUUID }

        ChainDeviceShell(
            title = "Pitcher",
            isSelected = isSelected,
            isDragging = isDragging.value,
            modifier = Modifier.width(150.dp),
            titleBarModifier = LocalTitleBarModifier.current
        ) {
            Box(
                modifier = Modifier.padding(all = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${state.value.pitch} semitones",
                    color = Theme[colors][primaryForeground],
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    override fun signalEnter(n: List<Signal>) {
        signalExit?.invoke(n.mapNotNull { signal ->
            if (signal is Signal.AudioSignal) {
                signal
            } else {
                val note = AbletonNoteSpace.note(signal) ?: return@mapNotNull null
                AbletonNoteSpace.withPitch(
                    signal = signal,
                    note = note,
                    pitch = (note.pitch + state.value.pitch).coerceIn(0, 127),
                )
            }
        })
    }

    companion object : ChainDeviceFactory<AbletonPitcherChainDeviceState> {
        override val stateClass = AbletonPitcherChainDeviceState::class
        override val serializer = AbletonPitcherChainDeviceState.serializer()
        override fun create() = AbletonPitcherChainDevice()
    }
}

@Serializable
data class AbletonPitcherChainDeviceState(
    val pitch: Int = 0
) : DeviceState()
