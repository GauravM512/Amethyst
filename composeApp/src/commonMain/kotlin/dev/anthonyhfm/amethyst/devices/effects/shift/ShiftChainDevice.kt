package dev.anthonyhfm.amethyst.devices.effects.shift

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.LEDChainDevice
import dev.anthonyhfm.amethyst.ui.components.AmethystDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

class ShiftChainDevice : LEDChainDevice<ShiftChainDeviceState>() {
    override val state = MutableStateFlow(ShiftChainDeviceState())

    @Composable
    override fun Content() {
        val selections by SelectionManager.selections.collectAsState()

        AmethystDevice(
            title = "Shift",
            isSelected = selections.any { it.selectionUUID == this.selectionUUID },
            isDragging = isDragging.value,
            modifier = Modifier.width(140.dp)
        ) {

        }
    }

    override fun ledSignalEnter(n: List<Signal.LED>) {

    }
}

@Serializable
data class ShiftChainDeviceState(
    val hue: Float = 0f,
    val saturationMax: Float = 1f,
    val saturationLow: Float = 0f,
    val valueHigh: Float = 1f,
    val valueLow: Float = 0f,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val temperature: Float = 0f,
    val invert: Boolean = false
) : DeviceState()