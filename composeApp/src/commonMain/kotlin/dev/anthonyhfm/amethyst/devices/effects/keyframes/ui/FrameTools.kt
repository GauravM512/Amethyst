package dev.anthonyhfm.amethyst.devices.effects.keyframes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesUIState
import dev.anthonyhfm.amethyst.ui.components.TextDial
import kotlin.math.roundToInt

@Composable
fun FrameTools(
    modifier: Modifier = Modifier,
    state: KeyframesUIState,
    onColorSelected: (Color) -> Unit,
    onFrameDurationChanged: (Double) -> Unit
) {
    val scrollState = rememberScrollState()
    val colorPickerController = rememberColorPickerController()

    LaunchedEffect(colorPickerController) {
        colorPickerController.selectByColor(
            color = state.drawColor,
            fromUser = false
        )
    }
    
    Card(
        modifier = modifier
            .padding(bottom = 24.dp)
            .padding(horizontal = 12.dp)
            .width(300.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HsvColorPicker(
                controller = colorPickerController,
                onColorChanged = { colorEnvelope ->
                    onColorSelected(colorEnvelope.color)
                },
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp)
            )
            
            BrightnessSlider(
                controller = colorPickerController,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                Modifier.padding(vertical = 8.dp),
            )
            
            Box(
                modifier = Modifier.padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val frameDuration = state.getFrameDuration(state.currentFrame)
                
                TextDial(
                    headline = "Duration",
                    text = "${frameDuration.roundToInt()} ms",
                    value = (frameDuration / 1000).toFloat(),
                    onValueChange = { value ->
                        onFrameDurationChanged((value * 1000).toDouble())
                    }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}
