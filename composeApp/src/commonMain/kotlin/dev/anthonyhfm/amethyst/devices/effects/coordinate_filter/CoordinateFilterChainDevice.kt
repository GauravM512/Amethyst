package dev.anthonyhfm.amethyst.devices.effects.coordinate_filter

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.heaven.elements.Signal
import dev.anthonyhfm.amethyst.devices.ChainDevice
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.ui.components.AmethystDevice
import dev.anthonyhfm.amethyst.workspace.WorkspaceController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

class CoordinateFilterChainDevice : ChainDevice<CoordinateFilterChainDeviceState>() {
    override val state = MutableStateFlow(CoordinateFilterChainDeviceState())

    val customMode: CoordinateFilterWorkspaceMode = CoordinateFilterWorkspaceMode()

    init {
        customMode.modeWakeup = {
            refreshVirtualDevices()
        }

        customMode.onVirtualDevicePress = { x, y, offset ->
            onSetKeyFilter(x, y, offset)
        }
    }

    @Composable
    override fun Content() {
        val controller = koinInject<WorkspaceController>()

        AmethystDevice(
            title = "Coordinate Filter",
            modifier = Modifier
                .width(200.dp)
        ) {
            Button(
                onClick = {
                    controller.switchMode(
                        mode =
                    )
                }
            ) {
                Text(
                    text = "Pick coordinates"
                )
            }
        }
    }

    fun refreshVirtualDevices() {
        Heaven.devices.forEach {

        }
    }

    fun onSetKeyFilter(x: Int, y: Int, offset: Offset) {

    }

    override fun midiEnter(n: List<Signal>) {
        midiExit?.invoke(n)
    }
}

@Serializable
data class CoordinateFilterChainDeviceState(
    val filters: List<Pair<Int, Int>> = emptyList()
) : DeviceState()