package dev.anthonyhfm.amethyst.editor.plugins.gradient

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.midi.data.MidiEffectData
import dev.anthonyhfm.amethyst.editor.plugins.EffectPlugin
import dev.anthonyhfm.amethyst.ui.components.AmethystPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class GradientPlugin : EffectPlugin() {
    override var isEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)

    private val gradientData: MutableStateFlow<Map<Float, Color>> = MutableStateFlow(
        value = mapOf(
            0f to Color.White,
            0.5f to Color.Red,
            1f to Color.Black
        )
    )

    private val gradientSteps: MutableStateFlow<Int> = MutableStateFlow(20)
    private val gradientDuration: MutableStateFlow<Int> = MutableStateFlow(300) // Duration in MS

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val colors = gradientData.collectAsState().value.toList().sortedBy { it.first }

        AmethystPlugin(
            title = "Gradient",
            enabled = isEnabled.collectAsState().value,
            onChangeEnabled = {
                scope.launch {
                    isEnabled.emit(it)
                }
            },
            modifier = Modifier
                .width(400.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(vertical = 12.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Canvas(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = colors.map { it.second },
                            startX = 0f,
                            endX = size.width
                        ),
                        size = size
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth()
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .offset(x = -10.dp)
                                .offset(
                                    x = maxWidth * color.first
                                )
                                .shadow(
                                    elevation = 6.dp,
                                    shape = CutCornerShape(topStartPercent = 50, topEndPercent = 50)
                                )
                                .clip(CutCornerShape(topStartPercent = 50, topEndPercent = 50))
                                .height(28.dp)
                                .width(20.dp)
                                .background(MaterialTheme.colorScheme.surfaceDim)
                                .border(2.dp, Color.White, CutCornerShape(topStartPercent = 50, topEndPercent = 50))
                        )
                    }
                }
            }
        }
    }

    override suspend fun passData(data: MidiEffectData) {
        if (data.r != 0 || data.g != 0 || data.b != 0) {
            CoroutineScope(Dispatchers.IO).launch {
                val stepLength = gradientDuration.value / gradientSteps.value
                val colors = gradientData.value.toList().sortedBy { it.first }

                for (step in 0..gradientSteps.value) {
                    val progress = step.toFloat() / gradientSteps.value
                    val color = interpolateGradient(colors, progress)

                    val midiData = data.copy(
                        r = (color.red * 63).toInt().coerceIn(0, 63),
                        g = (color.green * 63).toInt().coerceIn(0, 63),
                        b = (color.blue * 63).toInt().coerceIn(0, 63)
                    )

                    midiOutput(midiData)

                    delay(stepLength.milliseconds)
                }
                midiOutput(data.copy(r = 0, g = 0, b = 0))
            }
        }
    }

    private fun interpolateGradient(gradient: List<Pair<Float, Color>>, progress: Float): Color {
        val (start, end) = gradient.zipWithNext().find { (a, b) -> progress in a.first..b.first }
            ?: return gradient.last().second

        val t = (progress - start.first) / (end.first - start.first)

        return Color(
            red = start.second.red * (1 - t) + end.second.red * t,
            green = start.second.green * (1 - t) + end.second.green * t,
            blue = start.second.blue * (1 - t) + end.second.blue * t
        )
    }
}