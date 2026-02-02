package dev.anthonyhfm.amethyst.devices.effects.color_filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.LEDChainDevice
import dev.anthonyhfm.amethyst.ui.components.AmethystDevice
import dev.anthonyhfm.amethyst.ui.components.StepTextDial
import dev.anthonyhfm.amethyst.ui.components.TextDial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ColorFilterChainDevice : LEDChainDevice<ColorFilterChainDeviceState>() {
    override val state = MutableStateFlow(ColorFilterChainDeviceState())

    @Composable
    override fun Content() {
        val deviceState by state.collectAsState()
        val selections by SelectionManager.selections.collectAsState()

        AmethystDevice(
            title = "Color Filter",
            isSelected = selections.any { it.selectionUUID == this.selectionUUID },
            isDragging = isDragging.value,
            modifier = Modifier.width(280.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    StepTextDial(
                        headline = "Hue",
                        text = "${deviceState.hue}°",
                        steps = List(361) { -180 + it },
                        value = deviceState.hue,
                        onValueChange = { value ->
                            state.update {
                                it.copy(hue = value)
                            }
                        },
                        onResolveTextValue = { text ->
                            text.toIntOrNull()?.coerceIn(-180, 180)
                        },
                        onFinishValueChange = {

                        }
                    )

                    TextDial(
                        headline = "Tolerance",
                        text = "${(deviceState.hueTolerance * 100).toInt()}%",
                        value = deviceState.hueTolerance,
                        onValueChange = { value ->
                            state.update {
                                it.copy(hueTolerance = value)
                            }
                        },
                        onResolveTextValue = {

                        },
                        onFinishValueChange = {

                        }
                    )
                }

                VerticalDivider(Modifier.fillMaxHeight(0.8f))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TextDial(
                        headline = "Saturation",
                        text = "${(deviceState.saturation * 100).toInt()}%",
                        value = deviceState.saturation,
                        onValueChange = { value ->
                            state.update {
                                it.copy(saturation = value)
                            }
                        },
                        onResolveTextValue = {

                        },
                        onFinishValueChange = {

                        }
                    )

                    TextDial(
                        headline = "Tolerance",
                        text = "${(deviceState.saturationTolerance * 100).toInt()}%",
                        value = deviceState.saturationTolerance,
                        onValueChange = { value ->
                            state.update {
                                it.copy(saturationTolerance = value)
                            }
                        },
                        onResolveTextValue = {

                        },
                        onFinishValueChange = {

                        }
                    )
                }

                VerticalDivider(Modifier.fillMaxHeight(0.8f))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TextDial(
                        headline = "Value",
                        text = "${(deviceState.value * 100).toInt()}%",
                        value = deviceState.value,
                        onValueChange = { value ->
                            state.update {
                                it.copy(value = value)
                            }
                        },
                        onResolveTextValue = {

                        },
                        onFinishValueChange = {

                        }
                    )

                    TextDial(
                        headline = "Tolerance",
                        text = "${(deviceState.valueTolerance * 100).toInt()}%",
                        value = deviceState.valueTolerance,
                        onValueChange = { value ->
                            state.update {
                                it.copy(valueTolerance = value)
                            }
                        },
                        onResolveTextValue = {

                        },
                        onFinishValueChange = {

                        }
                    )
                }
            }
        }
    }

    override fun ledSignalEnter(n: List<Signal.LED>) {
        val s = state.value

        val filtered = n.filter { i ->
            if (i.color == Color.Transparent || i.color.alpha == 0f) return@filter true

            val (h, sat, v) = i.color.toHsv()

            val targetHue = (s.hue.toFloat() + 360f) % 360f
            val hueDiff = 180f - abs(abs(h - targetHue) - 180f)
            val hueMatch = (hueDiff / 180f) <= s.hueTolerance
            val satMatch = abs(sat - s.saturation) <= s.saturationTolerance
            val valMatch = abs(v - s.value) <= s.valueTolerance

            hueMatch && satMatch && valMatch
        }

        if (filtered.isNotEmpty()) {
            signalExit?.invoke(filtered)
        }
    }
}

private fun Color.toHsv(): Triple<Float, Float, Float> {
    val max = max(red, max(green, blue))
    val min = min(red, min(green, blue))
    val delta = max - min

    var h = 0f
    val s: Float = if (max == 0f) 0f else delta / max
    val v = max

    if (delta != 0f) {
        h = when (max) {
            red -> ((green - blue) / delta) % 6f
            green -> ((blue - red) / delta) + 2f
            else -> ((red - green) / delta) + 4f
        } * 60f

        if (h < 0f) h += 360f
    }

    return Triple(h, s, v)
}

@Serializable
data class ColorFilterChainDeviceState(
    val hue: Int = 0,
    val hueTolerance: Float = 0.05f,
    val saturation: Float = 1f,
    val saturationTolerance: Float = 0.05f,
    val value: Float = 1f,
    val valueTolerance: Float = 0.05f
) : DeviceState()