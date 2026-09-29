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

class AbletonChordChainDevice : GenericChainDevice<AbletonChordChainDeviceState>() {
    override val state = MutableStateFlow(AbletonChordChainDeviceState())

    override fun timelineDuration(context: TimelineDurationContext): TimelineDuration = TimelineDuration.None

    @Composable
    override fun Content() {
        val selections by SelectionManager.selections.collectAsState()
        val isSelected = selections.any { it.selectionUUID == selectionUUID }

        ChainDeviceShell(
            title = "Chord",
            isSelected = isSelected,
            isDragging = isDragging.value,
            modifier = Modifier.width(150.dp),
            titleBarModifier = LocalTitleBarModifier.current,
        ) {
            Box(
                modifier = Modifier.padding(all = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.value.shifts.joinToString(separator = ", ") { shift ->
                        if (shift > 0) "+$shift" else "$shift"
                    },
                    color = Theme[colors][primaryForeground],
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    override fun signalEnter(n: List<Signal>) {
        val output = n.flatMap { signal ->
            if (signal is Signal.AudioSignal) {
                listOf(signal)
            } else {
                val note = AbletonNoteSpace.note(signal)
                if (note == null) {
                    emptyList()
                } else {
                    state.value.shifts.mapNotNull { shift ->
                        AbletonNoteSpace.withPitch(
                            signal = signal,
                            note = note,
                            pitch = note.pitch + shift,
                        )
                    }
                }
            }
        }

        if (output.isNotEmpty()) {
            signalExit?.invoke(output)
        }
    }

    companion object : ChainDeviceFactory<AbletonChordChainDeviceState> {
        override val stateClass = AbletonChordChainDeviceState::class
        override val serializer = AbletonChordChainDeviceState.serializer()
        override fun create() = AbletonChordChainDevice()
    }
}

@Serializable
data class AbletonChordChainDeviceState(
    val shifts: List<Int> = listOf(0),
) : DeviceState()
