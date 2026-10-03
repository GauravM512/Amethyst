package dev.anthonyhfm.amethyst.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaveformViewportTest {
    @Test
    fun selectionFillsViewportWithDensityScaledMargins() {
        listOf(1f, 2f, 3f).forEach { density ->
            val width = 600f * density
            val padding = 16f * density
            val viewport = WaveformViewport.fitSelection(
                selectionStart = 0.4f,
                selectionEnd = 0.401f,
                width = width,
                padding = padding,
                minimumSpan = 0.0000001,
            )

            assertEquals(padding, viewport.screenX(position = 0.4f.toDouble(), width = width), 0.001f)
            assertEquals(width - padding, viewport.screenX(position = 0.401f.toDouble(), width = width), 0.001f)
        }
    }

    @Test
    fun shortSelectionInLongSongCanZoomBeyondOldLimit() {
        val viewport = WaveformViewport.fitSelection(
            selectionStart = 0.8f,
            selectionEnd = 0.80001f,
            width = 600f,
            padding = 16f,
            minimumSpan = 0.0000001,
        )

        assertTrue(viewport.span < 0.0001)
        assertEquals(16f, viewport.screenX(position = 0.8f.toDouble(), width = 600f), 0.001f)
        assertEquals(584f, viewport.screenX(position = 0.80001f.toDouble(), width = 600f), 0.001f)
    }

    @Test
    fun fitRemainsInsideSourceAtEitherBoundary() {
        listOf(0f to 0.01f, 0.99f to 1f, 0f to 1f).forEach { (start, end) ->
            val viewport = WaveformViewport.fitSelection(
                selectionStart = start,
                selectionEnd = end,
                width = 600f,
                padding = 16f,
                minimumSpan = 0.0000001,
            )

            assertTrue(viewport.start >= 0.0)
            assertTrue(viewport.end <= 1.0)
            assertTrue(viewport.start <= start.toDouble())
            assertTrue(viewport.end >= end.toDouble())
        }
    }

    @Test
    fun zoomKeepsSourcePositionUnderPointer() {
        val viewport = WaveformViewport(start = 0.4, span = 0.2)
        val zoomed = viewport.zoom(scale = 2f, anchorFraction = 0.75, minimumSpan = 0.0000001)

        assertEquals(viewport.positionAt(x = 450f, width = 600f), zoomed.positionAt(x = 450f, width = 600f), 0.000000001)
        assertEquals(0.1, zoomed.span, 0.000000001)
        val restored = zoomed.zoom(scale = 0.5f, anchorFraction = 0.75, minimumSpan = 0.0000001)
        assertEquals(viewport.start, restored.start, 0.000000001)
        assertEquals(viewport.span, restored.span, 0.000000001)
    }

    @Test
    fun repeatedZoomClampsWithoutLosingPrecisionOrDriftingAtLimit() {
        var viewport = WaveformViewport(start = 0.8, span = 0.1)
        repeat(times = 100) {
            viewport = viewport.zoom(scale = 2f, anchorFraction = 0.5, minimumSpan = 0.0000001)
        }

        assertEquals(0.0000001, viewport.span)
        val position = viewport.positionAt(x = 300f, width = 600f)
        assertEquals(0.85, position, 0.000000001)
        assertEquals(viewport, viewport.zoom(scale = 2f, anchorFraction = 0.5, minimumSpan = 0.0000001))
        assertEquals(300f, viewport.screenX(position = position, width = 600f), 0.001f)
    }

    @Test
    fun panningClampsAtSourceEdgesWithoutChangingZoom() {
        val viewport = WaveformViewport(start = 0.4, span = 0.2)

        assertEquals(WaveformViewport(start = 0.0, span = 0.2), viewport.pan(delta = -1.0))
        assertEquals(WaveformViewport(start = 0.8, span = 0.2), viewport.pan(delta = 1.0))
        assertEquals(WaveformViewport(), viewport.zoom(scale = 0.01f, anchorFraction = 0.5, minimumSpan = 0.0000001))
    }

    @Test
    fun edgePanningUsesOnlyGuttersAndIsIndependentOfFrameRate() {
        fun delta(pointerX: Float, elapsedSeconds: Float): Double = waveformEdgePanDelta(
            pointerX = pointerX,
            width = 600f,
            padding = 16f,
            span = 0.1,
            elapsedSeconds = elapsedSeconds,
        )

        assertEquals(0.0, delta(pointerX = 300f, elapsedSeconds = 1f))
        assertEquals(0.0, delta(pointerX = 16f, elapsedSeconds = 1f))
        assertEquals(0.0, delta(pointerX = 584f, elapsedSeconds = 1f))
        assertEquals(-0.15, delta(pointerX = 0f, elapsedSeconds = 1f), 0.000001)
        assertEquals(0.15, delta(pointerX = 600f, elapsedSeconds = 1f), 0.000001)
        assertEquals(delta(pointerX = 592f, elapsedSeconds = 1f), delta(pointerX = 592f, elapsedSeconds = 1f / 60f) * 60, 0.000001)
        assertEquals(delta(pointerX = 600f, elapsedSeconds = 1f), delta(pointerX = 900f, elapsedSeconds = 1f))
    }

    @Test
    fun fittingBeforeLayoutUsesFullSource() {
        assertEquals(
            WaveformViewport(),
            WaveformViewport.fitSelection(
                selectionStart = 0.4f,
                selectionEnd = 0.5f,
                width = 0f,
                padding = 16f,
                minimumSpan = 0.0000001,
            ),
        )
    }
}
