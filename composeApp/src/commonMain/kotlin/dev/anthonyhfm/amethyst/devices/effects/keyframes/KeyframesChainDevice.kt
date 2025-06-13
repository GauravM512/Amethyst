package dev.anthonyhfm.amethyst.devices.effects.keyframes

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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

class KeyframesChainDevice : ChainDevice<KeyframesChainDeviceState>() {
    override val state = MutableStateFlow(KeyframesChainDeviceState())
    private val viewModel: KeyframesWorkspaceModeViewModel = KeyframesWorkspaceModeViewModel()
    private val customMode: KeyframesWorkspaceMode = KeyframesWorkspaceMode(
        keyframesChainDevice = this,
        viewModel = viewModel
    )

    init {
        customMode.modeWakeup = {
            refreshVirtualDevices()
        }

        customMode.modeClose = {
            Heaven.devices.forEach { device ->
                device.previewState.clear()
            }
        }

        customMode.onVirtualDevicePress = { x, y, offset ->
            onSetLight(x, y, offset)
        }
    }

    @Composable
    override fun Content() {
        val controller = koinInject<WorkspaceRepository>()

        AmethystDevice(
            title = "Keyframes",
            modifier = Modifier
                .width(200.dp)
        ) {
            Button(
                onClick = {
                    controller.switchMode(
                        mode = customMode
                    )
                }
            ) {
                Text(
                    text = "Edit keyframes",
                )
            }
        }
    }

    fun refreshVirtualDevices() {
        Heaven.devices.forEach { device ->
            device.previewState.clear()

            val currentFrame = viewModel.state.value.currentFrame
            val currentFrameEntries = state.value.entries.find { it.id == currentFrame }?.entries ?: emptyList()

            currentFrameEntries.forEach { entry ->
                if (entry.x >= device.position.value.x.toInt() &&
                    entry.x < device.position.value.x.toInt() + 10 &&
                    entry.y >= device.position.value.y.toInt() && 
                    entry.y < device.position.value.y.toInt() + 10) {

                    val localX = entry.x - device.position.value.x.toInt()
                    val localY = 9 - (entry.y - device.position.value.y.toInt())

                    device.previewState.sendToPreview(listOf(
                        RawUpdate(localX + localY * 10, Color(entry.r, entry.g, entry.b))
                    ))
                }
            }
        }
    }

    fun onSetLight(x: Int, y: Int, offset: Offset) {
        val globalX = offset.x.toInt() + x
        val globalY = offset.y.toInt() + (9 - y)
        
        val currentFrameId = viewModel.state.value.currentFrame
        val selectedColor = viewModel.state.value.drawColor
        val frameDuration = viewModel.state.value.getFrameDuration(currentFrameId)
        
        viewModel.addColorToHistory(selectedColor)
        
        state.update { currentState ->
            val currentFrameIndex = currentState.entries.indexOfFirst { it.id == currentFrameId }
            
            if (currentFrameIndex == -1) {
                val newFrame = Frame(
                    id = currentFrameId,
                    entries = listOf(
                        KeyframeEntry(
                            x = globalX,
                            y = globalY,
                            r = selectedColor.red,
                            g = selectedColor.green,
                            b = selectedColor.blue
                        )
                    ),
                    length = frameDuration.toInt()
                )
                currentState.copy(
                    entries = currentState.entries + newFrame
                )
            } else {
                val currentFrame = currentState.entries[currentFrameIndex]
                val entryIndex = currentFrame.entries.indexOfFirst { it.x == globalX && it.y == globalY }
                
                val updatedFrame = if (entryIndex != -1) {
                    currentFrame.copy(
                        entries = currentFrame.entries.filterIndexed { index, _ -> index != entryIndex },
                        length = frameDuration.toInt() // Aktualisiere auch die Frame-Dauer
                    )
                } else {
                    val newEntry = KeyframeEntry(
                        x = globalX,
                        y = globalY,
                        r = selectedColor.red,
                        g = selectedColor.green,
                        b = selectedColor.blue
                    )
                    currentFrame.copy(
                        entries = currentFrame.entries + newEntry,
                        length = frameDuration.toInt()
                    )
                }
                
                val updatedEntries = currentState.entries.toMutableList().also {
                    it[currentFrameIndex] = updatedFrame
                }
                
                currentState.copy(entries = updatedEntries)
            }
        }

        refreshVirtualDevices()
    }

    fun updateFrameDuration(frameDuration: Double) {
        val currentFrameId = viewModel.state.value.currentFrame
        
        viewModel.setFrameDuration(currentFrameId, frameDuration)
        
        state.update { currentState ->
            val currentFrameIndex = currentState.entries.indexOfFirst { it.id == currentFrameId }
            
            if (currentFrameIndex != -1) {
                val updatedFrame = currentState.entries[currentFrameIndex].copy(
                    length = frameDuration.toInt()
                )
                
                val updatedEntries = currentState.entries.toMutableList().also {
                    it[currentFrameIndex] = updatedFrame
                }
                
                currentState.copy(entries = updatedEntries)
            } else {
                val newFrame = Frame(
                    id = currentFrameId,
                    entries = emptyList(),
                    length = frameDuration.toInt()
                )
                currentState.copy(
                    entries = currentState.entries + newFrame
                )
            }
        }
    }

    override fun midiEnter(n: List<Signal>) {
        midiExit?.invoke(n)
    }
}

@Serializable
data class KeyframesChainDeviceState(
    val entries: List<Frame> = emptyList()
) : DeviceState()

@Serializable
data class Frame(
    val id: Int,
    val entries: List<KeyframeEntry> = emptyList(),
    val length: Int = 200,
)

@Serializable
data class KeyframeEntry(
    val x: Int,
    val y: Int,
    val r: Float,
    val g: Float,
    val b: Float
)
