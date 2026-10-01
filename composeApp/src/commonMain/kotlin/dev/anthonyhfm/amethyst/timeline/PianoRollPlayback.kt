package dev.anthonyhfm.amethyst.timeline

import dev.anthonyhfm.amethyst.timeline.viewport.EditorViewportState

internal fun followPianoRollPlayhead(
    viewport: EditorViewportState,
    positionMs: Long,
): EditorViewportState {
    if (viewport.viewportWidth <= 0f) {
        return viewport
    }
    val screenX = viewport.timeMsToScreenX(timeMs = positionMs.toDouble())
    val rightEdge = viewport.viewportWidth * 0.9f
    if (screenX in 0f..rightEdge) {
        return viewport
    }
    return viewport.withConstrainedViewport(
        scrollX = viewport.timeMsToContentX(timeMs = positionMs.toDouble()) - viewport.viewportWidth * 0.1f,
    )
}
