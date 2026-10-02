package dev.anthonyhfm.amethyst.devices.ableton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.engine.elements.isSilentReplay
import dev.anthonyhfm.amethyst.core.engine.elements.isOn
import dev.anthonyhfm.amethyst.core.engine.heaven.Heaven
import dev.anthonyhfm.amethyst.core.util.Timing
import dev.anthonyhfm.amethyst.devices.ChainDeviceFactory
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.GenericChainDevice
import dev.anthonyhfm.amethyst.ui.components.primitives.ChainDeviceShell
import dev.anthonyhfm.amethyst.ui.components.toExactMsValue
import dev.anthonyhfm.amethyst.devices.TimelineDuration
import dev.anthonyhfm.amethyst.devices.TimelineDurationContext
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.primaryForeground
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import dev.anthonyhfm.amethyst.workspace.chain.ui.LocalTitleBarModifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

class AbletonArpeggiatorChainDevice : GenericChainDevice<AbletonArpeggiatorChainDeviceState>() {
    override val state = MutableStateFlow(AbletonArpeggiatorChainDeviceState())
    private val activeOutputs = mutableSetOf<Signal>()
    private val heldInputs = mutableSetOf<AbletonNoteSpace.Note>()
    private val patternInputs = mutableMapOf<AbletonNoteSpace.Note, Signal>()

    override fun timelineDuration(context: TimelineDurationContext): TimelineDuration {
        val current = state.value
        val rateMs = current.rate.toExactMsValue(context.bpm)
        val totalNotes = (current.steps.coerceAtLeast(0) + 1L) *
            (current.repeats ?: 1).coerceAtLeast(1)
        val duration = (totalNotes - 1L) * rateMs +
            rateMs * (current.gate / 100f)
        return TimelineDuration.Finite(duration.roundToLong().coerceAtLeast(0L))
    }

    @Composable
    override fun Content() {
        val selections by SelectionManager.selections.collectAsState()
        val isSelected = selections.any { it.selectionUUID == selectionUUID }

        ChainDeviceShell(
            title = "Arpeggiator",
            isSelected = isSelected,
            isDragging = isDragging.value,
            modifier = Modifier.width(160.dp),
            titleBarModifier = LocalTitleBarModifier.current
        ) {
            Box(
                modifier = Modifier.padding(all = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${state.value.steps + 1} steps · ${state.value.distance} st",
                    color = Theme[colors][primaryForeground],
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    override fun signalEnter(n: List<Signal>) {
        if (n.isSilentReplay()) {
            signalExit?.invoke(n)
            return
        }

        var shouldFlush = false
        n.forEach { signal ->
            if (signal is Signal.AudioSignal) {
                signalExit?.invoke(listOf(signal))
                return@forEach
            }

            val note = AbletonNoteSpace.note(signal) ?: return@forEach
            val current = state.value

            if (signal.isOn()) {
                if (current.hold && heldInputs.isEmpty()) {
                    patternInputs.clear()
                }

                heldInputs.add(note)
                patternInputs[note] = signal
                shouldFlush = true
            } else {
                heldInputs.remove(note)

                if (!current.hold) {
                    patternInputs.remove(note)
                    shouldFlush = true
                }
            }
        }

        if (shouldFlush) {
            flushPattern()
        }
    }

    private fun flushPattern() {
        Heaven.cancelJobsForOwner(owner = this)
        activeOutputs.toList().forEach { active ->
            signalExit?.invoke(listOf(offSignal(active)))
        }
        activeOutputs.clear()

        val current = state.value
        val notes = patternInputs.entries.sortedWith(
            compareBy<Map.Entry<AbletonNoteSpace.Note, Signal>> { it.key.pitch }
                .thenBy { it.key.targetX }
                .thenBy { it.key.targetY }
        ).let { sorted ->
            if (current.mode == 1) sorted.reversed() else sorted
        }
        val rateMs = current.rate.toExactMsValue(WorkspaceRepository.bpm.value)
        val gateMs = rateMs * (current.gate / 100f)
        val cycleLength = current.steps.coerceAtLeast(0) + 1
        val repeats = (current.repeats ?: 1).coerceAtLeast(1)
        var index = 0

        repeat(repeats) {
            repeat(cycleLength) { step ->
                notes.forEach { (note, signal) ->
                    val output = AbletonNoteSpace.withPitch(
                        signal = signal,
                        note = note,
                        pitch = note.pitch + step * current.distance,
                    )

                    if (output != null) {
                        Heaven.schedule(
                            delayInMs = rateMs * index,
                            owner = this,
                        ) {
                            val on = when (output) {
                                is Signal.LED -> output.copy(
                                    color = current.color?.let { color ->
                                        Color(
                                            red = color.first,
                                            green = color.second,
                                            blue = color.third,
                                        )
                                    } ?: output.color,
                                )
                                is Signal.Midi -> output
                                is Signal.AudioSignal -> output
                            }
                            activeOutputs.add(on)
                            signalExit?.invoke(listOf(on))

                            Heaven.schedule(
                                delayInMs = gateMs,
                                owner = this,
                            ) {
                                activeOutputs.remove(on)
                                signalExit?.invoke(listOf(offSignal(on)))
                            }
                        }
                    }

                    index++
                }
            }
        }
    }

    private fun offSignal(signal: Signal): Signal = when (signal) {
        is Signal.LED -> signal.copy(color = Color.Black)
        is Signal.Midi -> signal.copy(velocity = 0)
        is Signal.AudioSignal -> signal
    }

    companion object : ChainDeviceFactory<AbletonArpeggiatorChainDeviceState> {
        override val stateClass = AbletonArpeggiatorChainDeviceState::class
        override val serializer = AbletonArpeggiatorChainDeviceState.serializer()
        override fun create() = AbletonArpeggiatorChainDevice()
    }
}

@Serializable
data class AbletonArpeggiatorChainDeviceState(
    val rate: Timing = Timing.Rythm(Timing.Rythm.RythmTiming._1_8),
    val mode: Int = 0,
    val distance: Int = 12,
    val steps: Int = 0,
    val repeats: Int? = null,
    val hold: Boolean = false,
    val color: Triple<Float, Float, Float>? = null,
    val gate: Float = 50f
) : DeviceState()
