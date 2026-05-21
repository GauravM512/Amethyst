package dev.anthonyhfm.amethyst.devices.ableton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
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
import dev.anthonyhfm.amethyst.core.engine.heaven.Heaven
import dev.anthonyhfm.amethyst.core.midi.data.DRUM_RACK_TO_XY
import dev.anthonyhfm.amethyst.core.midi.data.XY_TO_DRUM_RACK
import dev.anthonyhfm.amethyst.core.util.Timing
import dev.anthonyhfm.amethyst.devices.ChainDeviceFactory
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.GenericChainDevice
import dev.anthonyhfm.amethyst.devices.LEDChainDevice
import dev.anthonyhfm.amethyst.ui.components.primitives.ChainDeviceShell
import dev.anthonyhfm.amethyst.ui.components.toMsValue
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.primaryForeground
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import dev.anthonyhfm.amethyst.workspace.chain.ui.LocalTitleBarModifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

class AbletonArpeggiatorChainDevice : LEDChainDevice<AbletonArpeggiatorChainDeviceState>() {
    override val state = MutableStateFlow(AbletonArpeggiatorChainDeviceState())

    @Composable
    override fun Content() {
        val selections by SelectionManager.selections.collectAsState()
        val isSelected = selections.any { it.selectionUUID == selectionUUID }

        ChainDeviceShell(
            title = "Arpeggiator",
            isSelected = isSelected,
            isDragging = isDragging.value,
            modifier = Modifier.width(240.dp),
            titleBarModifier = LocalTitleBarModifier.current
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This device's origin is the Ableton Converter. It is not interactable and only used for simulating Abletons Arpeggiator.",
                    color = Theme[colors][primaryForeground],
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                )
            }
        }
    }

    override fun ledSignalEnter(n: List<Signal.LED>) {
        Heaven.devices.forEach { device ->
            val onDeviceSignals = n.filter {
                val pos = device.position.value

                pos.x <= it.x && pos.x + device.layout.cols >= it.x && pos.y <= it.y && pos.y + device.layout.cols >= it.y
            }

            onDeviceSignals.forEach { signal ->
                val pos = device.position.value

                val signalX = signal.x - pos.x
                val signalY = signal.y - pos.y

                val local = (signalX + ((9 - (signalY)) * 10)).toInt()

                repeat(state.value.steps) { index ->
                    Heaven.schedule(
                        delayInMs = state.value.rate.toMsValue(WorkspaceRepository.bpm.value).toDouble() * index,
                    ) {
                        val drIndex: Int = XY_TO_DRUM_RACK[local] + index

                        val newX = DRUM_RACK_TO_XY[drIndex] % 10
                        val newY = 9 - DRUM_RACK_TO_XY[drIndex] / 10

                        signalExit?.invoke(
                            listOf(
                                signal.copy(
                                    x = newX,
                                    y = newY,
                                    color = state.value.color?.let {
                                        Color(
                                            red = it.first,
                                            green = it.second,
                                            blue = it.second,
                                        )
                                    } ?: signal.color
                                )
                            )
                        )

                        Heaven.schedule(
                            delayInMs = (state.value.rate.toMsValue(WorkspaceRepository.bpm.value).toDouble() * (state.value.gate / 100f)),
                        ) {
                            signalExit?.invoke(listOf(signal.copy(x = newX, y = newY, color = Color.Black)))
                        }
                    }
                }
            }
        }
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
    val distance: Int = 12,
    val steps: Int = 0,
    val repeats: Int? = null,
    val color: Triple<Float, Float, Float>? = null,
    val gate: Float = 50f // ableton handles this differently
) : DeviceState()

