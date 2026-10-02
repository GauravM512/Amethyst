package dev.anthonyhfm.amethyst.timeline.viewport

import dev.anthonyhfm.amethyst.timeline.utils.computeTimelineContentWidthPx

internal object TimelineViewportLimits {
    const val MIN_ZOOM_X = 0.0025f
    const val MAX_ZOOM_X = 100f
}

internal fun zoomTimelineViewport(
    viewport: EditorViewportState,
    scaleDelta: Float,
    anchorPx: Float,
    maxTimelineEndMs: Double,
    trailingMarginPx: Float,
): EditorViewportState {
    val zoomX = (viewport.zoomX * scaleDelta).coerceIn(viewport.minZoomX, viewport.maxZoomX)
    val contentWidth = computeTimelineContentWidthPx(
        maxTimelineEndMs = maxTimelineEndMs,
        zoomX = zoomX,
        viewportWidthPx = viewport.viewportWidth,
        trailingMarginPx = trailingMarginPx,
    )
    return viewport.copy(contentWidth = maxOf(viewport.contentWidth, contentWidth))
        .zoomAtX(scaleDelta = scaleDelta, focusScreenX = anchorPx)
        .setContentExtent(width = contentWidth, height = viewport.contentHeight)
}
