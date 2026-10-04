package dev.anthonyhfm.amethyst.ui.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

@Composable
internal fun Modifier.scaleGestureZoom(
    onZoom: (scaleFactor: Float, position: Offset) -> Unit,
): Modifier {
    val currentOnZoom by rememberUpdatedState(newValue = onZoom)

    return pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                if (
                    event.type != PointerEventType.ScaleStart &&
                    event.type != PointerEventType.ScaleChange &&
                    event.type != PointerEventType.ScaleEnd
                ) {
                    continue
                }

                val change = event.changes.firstOrNull() ?: continue
                if (event.changes.any { it.isConsumed }) {
                    continue
                }

                if (event.type == PointerEventType.ScaleChange) {
                    val scaleFactor = event.changes.fold(initial = 1f) { factor, pointer ->
                        factor * pointer.scaleFactor
                    }

                    if (scaleFactor.isFinite() && scaleFactor > 0f && scaleFactor != 1f) {
                        currentOnZoom(scaleFactor, change.position)
                    }
                }

                event.changes.forEach { it.consume() }
            }
        }
    }
}
