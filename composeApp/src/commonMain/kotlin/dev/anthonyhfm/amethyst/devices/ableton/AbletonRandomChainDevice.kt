package dev.anthonyhfm.amethyst.devices.ableton

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.engine.elements.isOn
import dev.anthonyhfm.amethyst.devices.ChainDeviceFactory
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.GenericChainDevice
import dev.anthonyhfm.amethyst.devices.TimelineDuration
import dev.anthonyhfm.amethyst.devices.TimelineDurationContext
import dev.anthonyhfm.amethyst.ui.components.primitives.ChainDeviceShell
import dev.anthonyhfm.amethyst.workspace.chain.ui.LocalTitleBarModifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlin.random.Random

class AbletonRandomChainDevice : GenericChainDevice<AbletonRandomChainDeviceState>() {
    override val state = MutableStateFlow(AbletonRandomChainDeviceState())
    override fun timelineDuration(context: TimelineDurationContext): TimelineDuration = TimelineDuration.None
    private val activePitches = mutableMapOf<AbletonNoteSpace.Note, MutableList<Int>>()
    private var alternateIndex = 0

    @Composable
    override fun Content() {
        val selections by SelectionManager.selections.collectAsState()

        ChainDeviceShell(
            title = "Ableton Random",
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
        val output = n.mapNotNull { signal ->
            if (signal is Signal.AudioSignal) {
                return@mapNotNull signal
            }

            val note = AbletonNoteSpace.note(signal) ?: return@mapNotNull null
            val pitch = if (signal.isOn()) {
                val selected = choosePitch(note.pitch, current)
                activePitches.getOrPut(note) { mutableListOf() }.add(selected)
                selected
            } else {
                val active = activePitches[note]
                if (active.isNullOrEmpty()) {
                    note.pitch
                } else {
                    val selected = active.removeAt(0)
                    if (active.isEmpty()) {
                        activePitches.remove(note)
                    }
                    selected
                }
            }

            AbletonNoteSpace.withPitch(
                signal = signal,
                note = note,
                pitch = pitch,
            )
        }

        if (output.isNotEmpty()) {
            signalExit?.invoke(output)
        }
    }

    internal fun choosePitch(input: Int, current: AbletonRandomChainDeviceState): Int {
        if (current.chance <= 0.0 || current.choices <= 0 || current.scale == 0) {
            return input
        }
        if (Random.nextDouble() >= current.chance.coerceIn(0.0, 1.0)) {
            return input
        }

        val choice = if (current.alternate) {
            val next = alternateIndex % current.choices
            alternateIndex++
            next
        } else {
            Random.nextInt(from = 1, until = current.choices + 1)
        }
        val distance = choice * current.scale
        val signedDistance = when (current.sign) {
            1 -> -distance
            2 -> if (Random.nextBoolean()) distance else -distance
            else -> distance
        }
        return (input + signedDistance).coerceIn(0, 127)
    }

    companion object : ChainDeviceFactory<AbletonRandomChainDeviceState> {
        override val stateClass = AbletonRandomChainDeviceState::class
        override val serializer = AbletonRandomChainDeviceState.serializer()
        override fun create() = AbletonRandomChainDevice()
    }
}

@Serializable
data class AbletonRandomChainDeviceState(
    val chance: Double = 0.0,
    val choices: Int = 1,
    val scale: Int = 1,
    val sign: Int = 0,
    val alternate: Boolean = false,
) : DeviceState()
