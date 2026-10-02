package dev.anthonyhfm.amethyst.timeline.viewport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class EditorViewportStateTest {
    @Test
    fun densityChangeKeepsVisibleTimeAndLogicalZoomStable() {
        val before = EditorViewportState(
            scrollX = 300f,
            zoomX = 0.5f,
            viewportWidth = 800f,
            contentWidth = 4_000f,
        )

        val after = before.rescaleHorizontalPixels(2f)

        assertEquals(before.screenToTimeMs(0f), after.screenToTimeMs(0f))
        assertEquals(600f, after.scrollX)
        assertEquals(1f, after.zoomX)
        assertEquals(1_600f, after.viewportWidth)
        assertEquals(8_000f, after.contentWidth)
    }

    @Test
    fun invalidDensityScaleDoesNotDamageViewport() {
        val viewport = EditorViewportState(scrollX = 42f, zoomX = 0.25f)

        assertSame(viewport, viewport.rescaleHorizontalPixels(1f))
        assertSame(viewport, viewport.rescaleHorizontalPixels(0f))
        assertSame(viewport, viewport.rescaleHorizontalPixels(Float.NaN))
    }

    @Test
    fun highZoomAndLongScrollPreserveSmallScreenOffsets() {
        val viewport = EditorViewportState(scrollX = 1_000_000_000f, zoomX = 100f)
        assertEquals(100f, viewport.timeMsToScreenX(timeMs = 10_000_001.0))
        assertEquals(12.5f, viewport.timeMsToScreenX(timeMs = 10_000_000.125))
        assertEquals(10_000_000.125, viewport.screenToTimeMs(screenX = 12.5f))
    }
}
