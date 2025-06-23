package dev.anthonyhfm.amethyst.devices.effects.keyframes

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.heaven.Heaven
import dev.anthonyhfm.amethyst.core.heaven.elements.RawUpdate
import dev.anthonyhfm.amethyst.core.heaven.elements.Signal
import dev.anthonyhfm.amethyst.devices.ChainDevice
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.ui.components.AmethystDevice
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

class KeyframesChainDevice : ChainDevice<KeyframesChainDeviceContract.KeyframesChainDeviceState>() {
    override val state = MutableStateFlow(KeyframesChainDeviceContract.KeyframesChainDeviceState())

    private val customMode: KeyframesWorkspaceMode = KeyframesWorkspaceMode()

    init {
        customMode.modeWakeup = {
            refreshVirtualDevices()
        }

        customMode.modeClose = {
            Heaven.devices.forEach { device ->
                device.previewState.clear()
            }
        }

        customMode.onVirtualDevicePress = { x, y, offset, size: IntSize ->
            onEvent(KeyframesChainDeviceContract.Event.OnPaintButton(x, y))
        }
    }

    @Composable
    override fun Content() {
        val controller = koinInject<WorkspaceRepository>()

        AmethystDevice(
            title = "Keyframes",
            modifier = Modifier
                .width(120.dp)
        ) {
            FilledIconButton(
                onClick = {
                    controller.switchMode(mode = customMode)
                },
                modifier = Modifier
                    .size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Draw,
                    contentDescription = "Draw",
                    modifier = Modifier
                        .size(36.dp)
                )
            }
        }
    }

    fun onEvent(event: KeyframesChainDeviceContract.Event) {
        when (event) {
            is KeyframesChainDeviceContract.Event.OnPaintButton -> {
                state.update { state ->
                    state.copy(
                        frames = state.frames.toMutableList().apply {
                            set(
                                index = state.selectedFrameIndex,
                                element = state.frames[state.selectedFrameIndex].copy(
                                    entries = state.frames[state.selectedFrameIndex].entries.toMutableList().apply {
                                        val index: Int = indexOfFirst { it.x == event.x && it.y == event.y }

                                        if (index != -1) {
                                            removeAt(index)
                                        }

                                        add(
                                            element = KeyframesChainDeviceContract.KeyframesEntry(
                                                x = event.x,
                                                y = event.y,
                                                r = state.selectedColor.first,
                                                g = state.selectedColor.second,
                                                b = state.selectedColor.third
                                            )
                                        )
                                    }
                                )
                            )
                        }
                    )
                }

                refreshVirtualDevices()
            }

            is KeyframesChainDeviceContract.Event.OnChangeFrameTiming -> {
                state.update {
                    it.copy(
                        frames = it.frames.toMutableList().apply {
                            set(
                                index = event.frameIndex,
                                element = it.frames[event.frameIndex].copy(timing = event.timing)
                            )
                        }
                    )
                }
            }

            is KeyframesChainDeviceContract.Event.OnColorUpdate -> {
                state.update {
                    it.copy(selectedColor = Triple(event.color.red, event.color.green, event.color.blue))
                }
            }

            is KeyframesChainDeviceContract.Event.OnSelectFrame -> {
                state.update {
                    it.copy(selectedFrameIndex = event.frameIndex)
                }
            }
        }
    }

    fun refreshVirtualDevices() {
        Heaven.devices.forEach { device ->
            device.previewState.clear()
        }

        println(state.value.frames[state.value.selectedFrameIndex]?.entries)

        Heaven.midiEnter(
            signals = state.value.frames[state.value.selectedFrameIndex]?.entries?.map { (x, y, r, g, b) ->
                Signal(
                    x = x,
                    y = y,
                    color = Color(r, g, b),
                    origin = this,
                )
            } ?: emptyList()
        )
    }

    /*fun onSetKeyFilter(x: Int, y: Int, offset: Offset) {
        val globalX = offset.x.toInt() + x
        val globalY = offset.y.toInt() + (9 - y)

        val coordinatePair = Pair(globalX, globalY)

        val isAlreadyFiltered = state.value.filters.contains(coordinatePair)

        state.update { currentState ->
            if (isAlreadyFiltered) {
                currentState.copy(
                    filters = currentState.filters.filter { it != coordinatePair }
                )
            } else {
                currentState.copy(
                    filters = currentState.filters + coordinatePair
                )
            }
        }

        refreshVirtualDevices()
    }*/

    override fun midiEnter(n: List<Signal>) {
        /*val filteredSignals = n.filter { signal ->
            state.value.filters.contains(Pair(signal.x, signal.y))
        }

        if (filteredSignals.isNotEmpty()) {
            midiExit?.invoke(filteredSignals)
        }*/
    }
}
