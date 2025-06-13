package dev.anthonyhfm.amethyst.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun Dial(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialValue by remember { mutableStateOf(value) }
    
    LaunchedEffect(value) {
        dialValue = value
    }

    LaunchedEffect(dialValue) {
        onValueChange(dialValue)
    }

    val background = MaterialTheme.colorScheme.surfaceColorAtElevation(32.dp)
    val dialColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .size(52.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { input, offset ->
                    dialValue = (dialValue + (offset * -1) * 0.01f).coerceIn(0f, 1f)
                }
            }
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(32.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceColorAtElevation(48.dp), CircleShape)
            .padding(6.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            drawArc(
                color = Color.LightGray.copy(0.2f),
                startAngle = 135f - 16f,
                sweepAngle = 270f + 32f,
                useCenter = true
            )

            drawArc(
                color = dialColor,
                startAngle = 135f - 16f,
                sweepAngle = (270f + 32f) * dialValue,
                useCenter = true
            )

            drawCircle(
                color = background,
                radius = 30f
            )
        }
    }
}

@Composable
fun TextDial(
    text: String,
    headline: String? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        headline?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Dial(
            modifier = modifier,
            value = value,
            onValueChange = {
                onValueChange(it)
            }
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun <T> StepDial(
    state: StepDialState<T>,
    modifier: Modifier = Modifier,
    label: ((T) -> String)? = null
) {
    var internalValue by remember { mutableStateOf(state.currentIndex.toFloat()) }

    // Aktualisiere den internen Wert, wenn sich der Index ändert
    LaunchedEffect(state.currentIndex) {
        internalValue = state.currentIndex.toFloat()
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .size(52.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, offset ->
                    // Kontinuierliche Aktualisierung des internen Werts für flüssiges Draggen
                    internalValue = (internalValue + (offset * -1) * 0.2f)

                    // Der interne Wert kann auch negative oder über (values.size-1) hinausgehen
                    // Das ermöglicht kontinuierliches Draggen ohne Begrenzung

                    // Berechne den neuen Index basierend auf dem internen Wert
                    val newIndex = internalValue.toInt().coerceIn(0, state.values.size - 1)
                    if (newIndex != state.currentIndex) {
                        state.currentIndex = newIndex
                    }
                }
            }
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(32.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceColorAtElevation(48.dp), CircleShape)
            .padding(6.dp)
    ) {
        val background = MaterialTheme.colorScheme.surfaceColorAtElevation(32.dp)
        val dialColor = MaterialTheme.colorScheme.tertiary

        // Die visuelle Darstellung ist auf den aktuellen Index beschränkt (also gesnapped)
        val visualValue = state.currentIndex.toFloat() / (state.values.size - 1).coerceAtLeast(1)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            drawArc(
                color = Color.LightGray.copy(0.2f),
                startAngle = 135f - 16f,
                sweepAngle = 270f + 32f,
                useCenter = true
            )

            drawArc(
                color = dialColor,
                startAngle = 135f - 16f,
                sweepAngle = (270f + 32f) * visualValue,
                useCenter = true
            )

            drawCircle(
                color = background,
                radius = 30f
            )
        }
    }
}

@Composable
fun <T> StepTextDial(
    state: StepDialState<T>,
    text: String,
    headline: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        headline?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall
            )
        }

        StepDial(
            modifier = modifier,
            state = state
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

class StepDialState<T>(
    val values: List<T>,
    initialIndex: Int = 0
) {
    var currentIndex by mutableStateOf(initialIndex.coerceIn(0, values.lastIndex))
    val current: T get() = values[currentIndex]
    fun next() { if (currentIndex < values.lastIndex) currentIndex++ }
    fun previous() { if (currentIndex > 0) currentIndex-- }
}

@Composable
fun <T> rememberStepDialState(
    values: List<T>,
    initialIndex: Int = 0
): StepDialState<T> {
    return remember(values, initialIndex) {
        StepDialState(values, initialIndex)
    }
}