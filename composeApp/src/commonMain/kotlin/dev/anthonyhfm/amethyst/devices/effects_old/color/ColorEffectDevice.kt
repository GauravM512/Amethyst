package dev.anthonyhfm.amethyst.devices.effects_old.color

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import dev.anthonyhfm.amethyst.core.midi.data.MidiEffectData
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects_old.EffectDevice
import dev.anthonyhfm.amethyst.ui.components.AmethystPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

class ColorEffectDevice : EffectDevice<ColorEffectDeviceState>() {
    override val state = MutableStateFlow(ColorEffectDeviceState())

    @Composable
    override fun Content() {
        val controller = rememberColorPickerController()

        LaunchedEffect(Unit) {
            controller.selectByColor(
                color = Color(
                    red = state.value.r,
                    green = state.value.g,
                    blue = state.value.b
                ),
                fromUser = false
            )
        }

        AmethystPlugin(
            title = "Color",
            modifier = Modifier
                .width(200.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HsvColorPicker(
                    controller = controller,
                    onColorChanged = { color ->
                        state.update {
                            it.copy(
                                r = color.color.red,
                                g = color.color.green,
                                b = color.color.blue
                            )
                        }
                    },
                    modifier = Modifier
                        .size(170.dp)
                )

                Spacer(Modifier.weight(1f))

                BrightnessSlider(
                    controller = controller,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )
            }
        }
    }

    override suspend fun passData(data: MidiEffectData) {
        if (data.r != 0 || data.g != 0 || data.b != 0) {
            state.value.let { color ->
                midiOutput(
                    data.copy(
                        r = (63 * color.r).toInt(),
                        g = (63 * color.g).toInt(),
                        b = (63 * color.b).toInt()
                    )
                )
            }
        } else {
            midiOutput(data)
        }
    }
}

@Serializable
data class ColorEffectDeviceState(
    val r: Float = 1f,
    val g: Float = 1f,
    val b: Float = 1f
) : DeviceState()