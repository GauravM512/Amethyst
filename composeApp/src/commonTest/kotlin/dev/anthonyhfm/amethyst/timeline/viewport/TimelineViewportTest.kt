package dev.anthonyhfm.amethyst.timeline.viewport

import kotlin.test.Test
import kotlin.test.assertEquals

class TimelineViewportTest {
    @Test
    fun timelineCanZoomTwentyTimesPastItsPreviousLimitWithoutLosingTheAnchor() {
        val viewport = EditorViewportState(
            zoomX = 5f,
            scrollX = 9000f,
            viewportWidth = 1000f,
            contentWidth = 10240f,
            minZoomX = TimelineViewportLimits.MIN_ZOOM_X,
            maxZoomX = TimelineViewportLimits.MAX_ZOOM_X,
        )
        val zoomed = zoomTimelineViewport(
            viewport = viewport,
            scaleDelta = 20f,
            anchorPx = 500f,
            maxTimelineEndMs = 2000.0,
            trailingMarginPx = 240f,
        )
        assertEquals(100f, zoomed.zoomX)
        assertEquals(viewport.screenToTimeMs(screenX = 500f), zoomed.screenToTimeMs(screenX = 500f))
        assertEquals(200240f, zoomed.contentWidth)
    }

    @Test
    fun zoomLimitStillPreservesTheCursorAnchor() {
        val viewport = EditorViewportState(
            zoomX = 80f,
            scrollX = 80000f,
            viewportWidth = 1000f,
            contentWidth = 160240f,
            maxZoomX = TimelineViewportLimits.MAX_ZOOM_X,
        )
        val zoomed = zoomTimelineViewport(
            viewport = viewport,
            scaleDelta = 10f,
            anchorPx = 500f,
            maxTimelineEndMs = 2000.0,
            trailingMarginPx = 240f,
        )
        assertEquals(100f, zoomed.zoomX)
        assertEquals(viewport.screenToTimeMs(screenX = 500f), zoomed.screenToTimeMs(screenX = 500f))
    }
}
