package dev.anthonyhfm.amethyst.workspace.modes.defaults

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.midi.data.MidiInputData
import dev.anthonyhfm.amethyst.core.util.Platform
import dev.anthonyhfm.amethyst.core.util.platform
import dev.anthonyhfm.amethyst.workspace.modes.WorkspaceMode
import dev.anthonyhfm.amethyst.workspace.ui.components.DesktopAutoPlayButtons
import dev.anthonyhfm.amethyst.workspace.ui.components.MobileAutoPlayButtons
import dev.anthonyhfm.amethyst.workspace.ui.viewport.ViewportConfig
import dev.anthonyhfm.amethyst.workspace.ui.viewport.ViewportPanBoundsPolicy
import dev.anthonyhfm.amethyst.workspace.ui.viewport.WorkspaceViewport

class PerformanceWorkspaceMode(
    override val displayName: String = "Performance"
) : WorkspaceMode() {
    override val selectableMode: Boolean = true

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return false
    }

    override fun onMidiInput(data: MidiInputData, offset: Offset) {

    }

    @Composable
    override fun Content(modifier: Modifier) {
        when (platform) {
            is Platform.Desktop -> DesktopLayout(modifier = modifier)

            else -> MobileLayout(modifier = modifier)
        }
    }

    @Composable
    private fun DesktopLayout(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
                .fillMaxSize()
        ) {
            WorkspaceViewport(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                viewportKey = "workspace-performance",
                config = ViewportConfig(
                    minZoom = 0.5f,
                    maxZoom = 2f,
                    enablePanning = true,
                    enableZoom = true,
                    draggableObjects = false,
                    panBoundsPolicy = ViewportPanBoundsPolicy.ClampToContent(
                        allowedOutOfBoundsFraction = 0.5f,
                    ),
                    showGrid = false,
                    showOrigin = false,
                    showActions = false,
                    showRemoteCursors = true,
                    contentPadding = 80.dp
                ),
            )

            DesktopAutoPlayButtons()
        }
    }

    @Composable
    private fun MobileLayout(modifier: Modifier = Modifier) {
        Column {
            WorkspaceViewport(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(32.dp)),
                viewportKey = "workspace-performance",
                config = ViewportConfig(
                    minZoom = 0.5f,
                    maxZoom = 2f,
                    enablePanning = true,
                    enableZoom = true,
                    draggableObjects = false,
                    panBoundsPolicy = ViewportPanBoundsPolicy.ClampToContent(
                        allowedOutOfBoundsFraction = 0.5f,
                    ),
                    showGrid = false,
                    showOrigin = false,
                    showActions = false,
                    showRemoteCursors = true,
                    contentPadding = 16.dp
                ),
            )

            MobileAutoPlayButtons()
        }
    }
}
