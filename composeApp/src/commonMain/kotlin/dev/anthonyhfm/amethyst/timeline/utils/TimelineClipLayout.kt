package dev.anthonyhfm.amethyst.timeline.utils

import dev.anthonyhfm.amethyst.timeline.viewport.EditorViewportState
import kotlin.math.roundToLong
import kotlin.math.roundToInt

internal data class TimelineProjectedSpanPx(
    val startPx: Int,
    val endPx: Int,
) {
    val widthPx: Int
        get() = (endPx - startPx).coerceAtLeast(1)
}

internal fun projectTimelineSpanPx(
    startTimeMs: Double,
    endTimeMs: Double,
    zoomX: Float,
): TimelineProjectedSpanPx {
    val startPx = (startTimeMs * zoomX.toDouble()).roundToInt()
    val endPx = (endTimeMs * zoomX.toDouble()).roundToInt().coerceAtLeast(startPx + 1)
    return TimelineProjectedSpanPx(
        startPx = startPx,
        endPx = endPx,
    )
}

internal fun projectTimelineTimeToScreenPx(timeMs: Long, zoomX: Float, scrollX: Float): Float =
    (timeMs.toDouble() * zoomX.toDouble() - scrollX.toDouble()).toFloat()

internal fun computeVisibleTimelineRangePx(
    startMs: Long,
    endMs: Long,
    viewport: EditorViewportState,
): TimelineVisibleClipWindowPx? {
    if (endMs <= startMs) {
        return null
    }
    val span = projectTimelineSpanPx(
        startTimeMs = startMs.toDouble(),
        endTimeMs = endMs.toDouble(),
        zoomX = viewport.zoomX,
    )
    return computeVisibleClipWindowPx(contentStartPx = span.startPx, contentEndPx = span.endPx, viewport = viewport)
}

internal fun computeTimelineContentWidthPx(
    maxTimelineEndMs: Double,
    zoomX: Float,
    viewportWidthPx: Float,
    trailingMarginPx: Float,
): Float {
    val projectedEndPx = if (maxTimelineEndMs > 0.0) {
        projectTimelineSpanPx(
            startTimeMs = 0.0,
            endTimeMs = maxTimelineEndMs,
            zoomX = zoomX,
        ).endPx.toFloat()
    } else {
        0f
    }

    val desiredWidthPx = if (projectedEndPx > 0f) {
        projectedEndPx + trailingMarginPx.coerceAtLeast(0f)
    } else {
        0f
    }

    return maxOf(viewportWidthPx.coerceAtLeast(0f), desiredWidthPx)
}

internal data class TimelineVisibleClipWindowPx(
    val screenStartPx: Int,
    val screenEndPx: Int,
    val visibleLeftPx: Int,
    val visibleRightPx: Int,
    val hiddenLeftPx: Int,
    val hiddenRightPx: Int,
    val visibleContentStartPx: Int,
    val visibleContentEndPx: Int,
) {
    val visibleWidthPx: Int
        get() = (visibleRightPx - visibleLeftPx).coerceAtLeast(0)

    val isLeftEdgeVisible: Boolean
        get() = hiddenLeftPx == 0

    val isRightEdgeVisible: Boolean
        get() = hiddenRightPx == 0
}

internal fun computeVisibleClipWindowPx(
    contentStartPx: Int,
    contentEndPx: Int,
    viewport: EditorViewportState,
    screenOffsetPx: Int = 0,
    cullPaddingPx: Int = 100,
    retainOffscreen: Boolean = false,
): TimelineVisibleClipWindowPx? {
    val viewportWidthPx = viewport.viewportWidth.roundToInt().coerceAtLeast(0)
    if (viewportWidthPx <= 0) return null

    val normalizedEndPx = contentEndPx.coerceAtLeast(contentStartPx + 1)
    val widthPx = normalizedEndPx - contentStartPx
    val screenStartPx = contentStartPx - viewport.scrollX.roundToInt() + screenOffsetPx
    val screenEndPx = screenStartPx + widthPx

    if (screenEndPx < -cullPaddingPx || screenStartPx > viewportWidthPx + cullPaddingPx) {
        if (!retainOffscreen) return null
        val retainedLeft = if (screenEndPx < 0) 0 else (viewportWidthPx - 1).coerceAtLeast(0)
        val retainedRight = (retainedLeft + 1).coerceAtMost(viewportWidthPx)
        return TimelineVisibleClipWindowPx(
            screenStartPx = screenStartPx,
            screenEndPx = screenEndPx,
            visibleLeftPx = retainedLeft,
            visibleRightPx = retainedRight,
            hiddenLeftPx = (retainedLeft - screenStartPx).coerceAtLeast(0),
            hiddenRightPx = (screenEndPx - retainedRight).coerceAtLeast(0),
            visibleContentStartPx = contentStartPx,
            visibleContentEndPx = (contentStartPx + 1).coerceAtMost(normalizedEndPx),
        )
    }

    val visibleLeftPx = screenStartPx.coerceAtLeast(0)
    val visibleRightPx = screenEndPx.coerceAtMost(viewportWidthPx)
    if (visibleRightPx <= visibleLeftPx) return null

    val hiddenLeftPx = (visibleLeftPx - screenStartPx).coerceAtLeast(0)
    val hiddenRightPx = (screenEndPx - visibleRightPx).coerceAtLeast(0)

    return TimelineVisibleClipWindowPx(
        screenStartPx = screenStartPx,
        screenEndPx = screenEndPx,
        visibleLeftPx = visibleLeftPx,
        visibleRightPx = visibleRightPx,
        hiddenLeftPx = hiddenLeftPx,
        hiddenRightPx = hiddenRightPx,
        visibleContentStartPx = contentStartPx + hiddenLeftPx,
        visibleContentEndPx = normalizedEndPx - hiddenRightPx,
    )
}

internal data class MidiClipTrimSpan(
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long
        get() = endMs - startMs
}

internal fun resolveMidiClipTrimSpan(
    startMs: Long,
    endMs: Long,
    leftDeltaPx: Float,
    rightDeltaPx: Float,
    zoomX: Float,
    snapTime: (Long) -> Long,
): MidiClipTrimSpan {
    val minimumDurationMs = minOf(50L, endMs - startMs).coerceAtLeast(1L)
    val safeZoomX = zoomX.coerceAtLeast(0.0001f)
    val trimmedStartMs = if (leftDeltaPx != 0f) {
        snapTime((startMs.toDouble() + leftDeltaPx.toDouble() / safeZoomX).roundToLong())
            .coerceIn(0L, (endMs - minimumDurationMs).coerceAtLeast(0L))
    } else {
        startMs
    }
    val trimmedEndMs = if (rightDeltaPx != 0f) {
        snapTime((endMs.toDouble() + rightDeltaPx.toDouble() / safeZoomX).roundToLong())
            .coerceAtLeast(trimmedStartMs + minimumDurationMs)
    } else {
        endMs
    }
    return MidiClipTrimSpan(startMs = trimmedStartMs, endMs = trimmedEndMs)
}
