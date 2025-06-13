package dev.anthonyhfm.amethyst.devices.effects.keyframes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import dev.anthonyhfm.amethyst.devices.effects.keyframes.ui.FrameControl
import dev.anthonyhfm.amethyst.devices.effects.keyframes.ui.FrameTools
import dev.anthonyhfm.amethyst.workspace.WorkspaceContract

class KeyframesWorkspaceMode(
    val keyframesChainDevice: KeyframesChainDevice,
    private val viewModel: KeyframesWorkspaceModeViewModel
) : WorkspaceContract.WorkspaceMode {
    override val displayName: String = "Keyframes Editor"
    override val selectable: Boolean = false

    var onVirtualDevicePress: ((x: Int, y: Int, offset: Offset) -> Unit)? = null
    var modeWakeup: (() -> Unit)? = null
    var modeClose: (() -> Unit)? = null

    fun virtualDevicePress(x: Int, y: Int, offset: Offset) {
        onVirtualDevicePress?.invoke(x, y, offset)
    }

    fun wake() {
        modeWakeup?.invoke()
    }

    fun close() {
        modeClose?.invoke()
    }

    @Composable
    fun EditorUI(paddingValues: PaddingValues) {
        val state by viewModel.state.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FrameControl(
                modifier = Modifier.align(Alignment.BottomCenter),
                currentFrame = state.currentFrame,
                isPlaying = state.isPlaying,
                onPreviousFrame = { 
                    viewModel.previousFrame()
                    keyframesChainDevice.refreshVirtualDevices()
                },
                onNextFrame = {
                    viewModel.nextFrame()
                    keyframesChainDevice.refreshVirtualDevices()
                },
                onPlayPause = {
                    // TODO: Play/pause logic
                }
            )

            FrameTools(
                state = state,
                modifier = Modifier.align(Alignment.TopEnd),
                onColorSelected = { color ->
                    viewModel.setDrawColor(color)
                },
                onFrameDurationChanged = { duration ->
                    keyframesChainDevice.updateFrameDuration(duration)
                }
            )
        }
    }
}
