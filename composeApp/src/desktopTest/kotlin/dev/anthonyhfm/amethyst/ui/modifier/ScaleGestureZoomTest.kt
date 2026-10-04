package dev.anthonyhfm.amethyst.ui.modifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalComposeUiApi::class)
class ScaleGestureZoomTest {
    @Test
    fun nativeScaleZoomsAtThePointerWithoutKeyboardModifiers() {
        val updates = mutableListOf<Pair<Float, Offset>>()
        val consumedEvents = mutableListOf<Boolean>()
        withScene(
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scaleGestureZoom { factor, position ->
                            updates += factor to position
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                    consumedEvents += event.changes.any { it.isConsumed }
                                }
                            }
                        }
                )
            }
        ) { scene, _ ->
            val anchor = Offset(x = 80f, y = 30f)
            listOf(
                PointerEventType.ScaleStart to 1f,
                PointerEventType.ScaleChange to 1.25f,
                PointerEventType.ScaleChange to 1f,
                PointerEventType.ScaleChange to 0.8f,
                PointerEventType.ScaleEnd to 1f,
            ).forEach { (type, factor) ->
                scene.sendPointerEvent(
                    eventType = type,
                    position = anchor,
                    keyboardModifiers = PointerKeyboardModifiers(),
                    scaleGestureFactor = factor,
                )
            }

            assertEquals(expected = 5, actual = consumedEvents.size)
            assertTrue(actual = consumedEvents.all { it })

            assertEquals(
                expected = listOf(1.25f to anchor, 0.8f to anchor),
                actual = updates,
            )

            scene.sendPointerEvent(
                eventType = PointerEventType.Scroll,
                position = anchor,
                scrollDelta = Offset(x = 0f, y = -1f),
                keyboardModifiers = PointerKeyboardModifiers(isCtrlPressed = true),
            )
            assertFalse(actual = consumedEvents.last())
            assertEquals(expected = 2, actual = updates.size)
        }
    }

    @Test
    fun nestedZoomHandlersApplyEachScaleOnlyOnce() {
        var outerZoom = 1f
        var innerZoom = 1f
        withScene(
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scaleGestureZoom { factor, _ -> outerZoom *= factor }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scaleGestureZoom { factor, _ -> innerZoom *= factor }
                    )
                }
            }
        ) { scene, _ ->
            scene.sendPointerEvent(
                eventType = PointerEventType.ScaleStart,
                position = Offset(x = 50f, y = 50f),
            )
            scene.sendPointerEvent(
                eventType = PointerEventType.ScaleChange,
                position = Offset(x = 50f, y = 50f),
                scaleGestureFactor = 1.5f,
            )
            scene.sendPointerEvent(
                eventType = PointerEventType.ScaleEnd,
                position = Offset(x = 50f, y = 50f),
            )

            assertEquals(expected = 1.5f, actual = outerZoom)
            assertEquals(expected = 1f, actual = innerZoom)
        }
    }

    @Test
    fun scaleUsesTheUpdatedCallbackDuringAGesture() {
        val generation = mutableStateOf(value = 0)
        val updates = mutableListOf<Int>()
        withScene(
            content = {
                val currentGeneration = generation.value
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scaleGestureZoom { _, _ -> updates += currentGeneration }
                )
            }
        ) { scene, recomposer ->
            scene.sendPointerEvent(
                eventType = PointerEventType.ScaleStart,
                position = Offset(x = 50f, y = 50f),
            )
            scene.sendPointerEvent(
                eventType = PointerEventType.ScaleChange,
                position = Offset(x = 50f, y = 50f),
                scaleGestureFactor = 1.5f,
            )

            generation.value = 1
            recomposer.performFrame(frameTimeNanos = 16_000_000L)
            scene.measureAndLayout()

            scene.sendPointerEvent(
                eventType = PointerEventType.ScaleChange,
                position = Offset(x = 50f, y = 50f),
                scaleGestureFactor = 0.8f,
            )

            assertEquals(expected = listOf(0, 1), actual = updates)
        }
    }

    private fun withScene(
        content: @Composable () -> Unit,
        block: (ComposeScene, FrameRecomposer) -> Unit,
    ) {
        val recomposer = FrameRecomposer(coroutineContext = Dispatchers.Unconfined)
        val scene = CanvasLayersComposeScene(
            frameRecomposer = recomposer,
            size = IntSize(width = 100, height = 100),
        )

        try {
            scene.setContent(content = content)
            recomposer.performFrame(frameTimeNanos = 0L)
            scene.measureAndLayout()
            block(scene, recomposer)
        } finally {
            scene.close()
            recomposer.close()
        }
    }
}
