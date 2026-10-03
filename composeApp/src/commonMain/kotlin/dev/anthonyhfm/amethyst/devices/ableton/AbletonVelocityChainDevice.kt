package dev.anthonyhfm.amethyst.devices.ableton

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.engine.elements.isOn
import dev.anthonyhfm.amethyst.core.engine.elements.isSilentReplay
import dev.anthonyhfm.amethyst.core.util.Palettes
import dev.anthonyhfm.amethyst.devices.ChainDeviceFactory
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.GenericChainDevice
import dev.anthonyhfm.amethyst.devices.TimelineDuration
import dev.anthonyhfm.amethyst.devices.TimelineDurationContext
import dev.anthonyhfm.amethyst.ui.components.primitives.ChainDeviceShell
import dev.anthonyhfm.amethyst.workspace.chain.ui.LocalTitleBarModifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt
import kotlin.random.Random

class AbletonVelocityChainDevice : GenericChainDevice<AbletonVelocityChainDeviceState>() {
    override val state = MutableStateFlow(AbletonVelocityChainDeviceState())
    private val forwardedNotes = mutableMapOf<Any, MutableList<Boolean>>()

    override fun timelineDuration(context: TimelineDurationContext) = TimelineDuration.None

    @Composable
    override fun Content() {
        val selections by SelectionManager.selections.collectAsState()
        val isSelected = selections.any { it.selectionUUID == selectionUUID }

        ChainDeviceShell(
            title = "Velocity",
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
        if (n.isSilentReplay()) {
            signalExit?.invoke(n)
            return
        }

        val current = state.value
        val result = n.mapNotNull { signal ->
            if (signal is Signal.AudioSignal) {
                return@mapNotNull signal
            }

            val noteKey = AbletonNoteSpace.note(signal) ?: when (signal) {
                is Signal.LED -> signal.x to signal.y
                is Signal.Midi -> signal.x to signal.y
                is Signal.AudioSignal -> return@mapNotNull signal
            }

            if (!signal.isOn()) {
                val active = forwardedNotes[noteKey]
                val wasForwarded = if (active.isNullOrEmpty()) {
                    current.mode != 1
                } else {
                    val forwarded = active.removeAt(0)
                    if (active.isEmpty()) {
                        forwardedNotes.remove(noteKey)
                    }
                    forwarded
                }
                if (!wasForwarded) {
                    return@mapNotNull null
                }
                return@mapNotNull signal
            }

            val velocity = when (signal) {
                is Signal.Midi -> signal.velocity
                is Signal.LED -> signal.extras[VELOCITY] ?: inferVelocity(signal.color, current)
                is Signal.AudioSignal -> return@mapNotNull signal
            }.coerceIn(1, 127)
            val minimumInput = current.lowest
            val maximumInput = current.lowest + current.range.coerceAtLeast(1) - 1
            val accepted = current.mode != 1 || velocity in minimumInput..maximumInput
            forwardedNotes.getOrPut(noteKey) { mutableListOf() }.add(accepted)
            if (!accepted) {
                return@mapNotNull null
            }

            val outputVelocity = if (current.mode == 2) {
                current.outHigh
            } else {
                val position = if (maximumInput == minimumInput) {
                    0f
                } else {
                    (velocity.coerceIn(minimumInput, maximumInput) - minimumInput).toFloat() /
                        (maximumInput - minimumInput)
                }
                (current.outLow + position * (current.outHigh - current.outLow)).roundToInt()
            }.let { base ->
                (base + if (current.random > 0) {
                    Random.nextInt(from = -current.random, until = current.random + 1)
                } else {
                    0
                }).coerceIn(0, 127)
            }

            when (signal) {
                is Signal.LED -> signal.copy(
                    color = paletteColor(outputVelocity, current),
                    extras = signal.extras + (VELOCITY to outputVelocity),
                )
                is Signal.Midi -> signal.copy(
                    velocity = outputVelocity,
                    extras = signal.extras + (VELOCITY to outputVelocity),
                )
                is Signal.AudioSignal -> signal
            }
        }

        if (result.isNotEmpty()) {
            signalExit?.invoke(result)
        }
    }

    private fun inferVelocity(color: Color, current: AbletonVelocityChainDeviceState): Int {
        if (color == Color.White) {
            return 127
        }

        return (1..127).minByOrNull { index ->
            val candidate = paletteColor(index, current)
            val red = candidate.red - color.red
            val green = candidate.green - color.green
            val blue = candidate.blue - color.blue
            red * red + green * green + blue * blue
        } ?: 127
    }

    private fun paletteColor(velocity: Int, current: AbletonVelocityChainDeviceState): Color {
        val packed = current.palette?.getOrNull(velocity)
        val (red, green, blue) = if (packed != null) {
            Triple((packed shr 12) and 63, (packed shr 6) and 63, packed and 63)
        } else {
            Palettes.novation[velocity]
        }

        return Color(
            red = red / 63f,
            green = green / 63f,
            blue = blue / 63f,
        )
    }

    companion object : ChainDeviceFactory<AbletonVelocityChainDeviceState> {
        const val VELOCITY = "ableton.velocity"
        override val stateClass = AbletonVelocityChainDeviceState::class
        override val serializer = AbletonVelocityChainDeviceState.serializer()
        override fun create() = AbletonVelocityChainDevice()
    }
}

@Serializable
data class AbletonVelocityChainDeviceState(
    val outLow: Int = 1,
    val outHigh: Int = 127,
    val lowest: Int = 1,
    val range: Int = 127,
    val mode: Int = 0,
    val random: Int = 0,
    val palette: List<Int>? = null,
) : DeviceState()
