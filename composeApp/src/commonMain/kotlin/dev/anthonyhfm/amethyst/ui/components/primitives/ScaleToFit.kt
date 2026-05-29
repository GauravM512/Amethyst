package dev.anthonyhfm.amethyst.ui.components.primitives

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints

/**
 * A layout container that measures its single child with infinite constraints (its natural size),
 * and then scales it down dynamically using graphics scale layer to fit the maximum width constraint.
 */
@Composable
fun ScaleToFit(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val placeable = measurables.firstOrNull()?.measure(Constraints()) ?: return@Layout layout(0, 0) {}
        val scale = if (placeable.width > 0) {
            constraints.maxWidth.toFloat() / placeable.width
        } else 1f
        val scaledWidth = (placeable.width * scale).toInt()
        val scaledHeight = (placeable.height * scale).toInt()
        layout(scaledWidth, scaledHeight) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}
