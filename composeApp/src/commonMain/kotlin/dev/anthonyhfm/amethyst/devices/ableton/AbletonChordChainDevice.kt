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
            modifier = Modifier
                .width(width = 150.dp),
            titleBarModifier = LocalTitleBarModifier.current,
        ) {
            AbletonCompatibilityNotice()
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
