package dev.anthonyhfm.amethyst.timeline

import androidx.compose.ui.geometry.Offset
import dev.anthonyhfm.amethyst.timeline.data.AudioEntry
import dev.anthonyhfm.amethyst.timeline.data.AudioTimelineTrack
import dev.anthonyhfm.amethyst.timeline.data.TimelineAutomationPoint
import dev.anthonyhfm.amethyst.timeline.data.TimelineTrackAutomationTarget
import dev.anthonyhfm.amethyst.timeline.ui.views.hitAutomationPoint
import dev.anthonyhfm.amethyst.timeline.utils.GridUtils
import dev.anthonyhfm.amethyst.timeline.utils.computeSnappedTimeFromViewport
import dev.anthonyhfm.amethyst.timeline.utils.computeVisibleClipWindowPx
import dev.anthonyhfm.amethyst.timeline.utils.computeVisibleTimelineRangePx
import dev.anthonyhfm.amethyst.timeline.utils.findHeaderEntryHit
import dev.anthonyhfm.amethyst.timeline.utils.projectTimelineSpanPx
import dev.anthonyhfm.amethyst.timeline.utils.projectTimelineTimeToScreenPx
import dev.anthonyhfm.amethyst.timeline.viewport.EditorViewportState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TimelineZoomAlignmentTest {
    @Test
    fun clipsAndRangesRemainAlignedWhenMuchWiderThanTheViewport() {
        listOf(1f, 5f, 20f, 100f).forEach { zoom ->
            val viewport = EditorViewportState(zoomX = zoom, scrollX = 900000f * zoom - 400f, viewportWidth = 1000f)
            val span = projectTimelineSpanPx(startTimeMs = 0.0, endTimeMs = 900000.0, zoomX = zoom)
            val clip = assertNotNull(
                computeVisibleClipWindowPx(contentStartPx = span.startPx, contentEndPx = span.endPx, viewport = viewport)
            )
            val range = assertNotNull(computeVisibleTimelineRangePx(startMs = 0L, endMs = 900000L, viewport = viewport))
            assertEquals(0, clip.visibleLeftPx)
            assertEquals(400, clip.visibleWidthPx)
            assertEquals(viewport.timeMsToScreenX(timeMs = 900000.0), clip.visibleRightPx.toFloat())
            assertEquals(clip, range)
        }
    }

    @Test
    fun rangesCoverOnlyTheirVisibleSectionAndInvalidRangesAreCulled() {
        val viewport = EditorViewportState(zoomX = 100f, scrollX = 100000f, viewportWidth = 800f)
        val whole = assertNotNull(computeVisibleTimelineRangePx(startMs = 0L, endMs = 2000L, viewport = viewport))
        assertEquals(0, whole.visibleLeftPx)
        assertEquals(800, whole.visibleWidthPx)
        assertNull(computeVisibleTimelineRangePx(startMs = 0L, endMs = 10L, viewport = viewport))
        assertNull(computeVisibleTimelineRangePx(startMs = 2000L, endMs = 2000L, viewport = viewport))
    }

    @Test
    fun midiEdgesAndAutomationPointsUseTheSamePixelAsTheRuler() {
        val viewport = EditorViewportState(zoomX = 100f, scrollX = 1_000_000_000f, viewportWidth = 800f)
        val span = projectTimelineSpanPx(startTimeMs = 10_000_001.0, endTimeMs = 10_000_006.0, zoomX = viewport.zoomX)
        val clip = assertNotNull(
            computeVisibleClipWindowPx(contentStartPx = span.startPx, contentEndPx = span.endPx, viewport = viewport)
        )
        assertEquals(100, clip.visibleLeftPx)
        assertEquals(600, clip.visibleRightPx)
        assertEquals(
            clip.visibleLeftPx.toFloat(),
            projectTimelineTimeToScreenPx(timeMs = 10_000_001L, zoomX = viewport.zoomX, scrollX = viewport.scrollX),
        )
        assertEquals(viewport.timeMsToScreenX(timeMs = 10_000_001.0), clip.visibleLeftPx.toFloat())
    }

    @Test
    fun automationHitTestingFindsThePointAtItsPreciseDrawnPosition() {
        val target = TimelineTrackAutomationTarget.VOLUME
        val point = TimelineAutomationPoint(
            timeMs = 10_000_001L,
            value = target.displayValueToValue(displayValue = target.displayProgressToDisplayValue(progress = 0.5f)),
        )
        val y = 100f - target.valueToDisplayProgress(value = point.value) * 100f
        assertEquals(
            point,
            hitAutomationPoint(
                points = listOf(point),
                tapOffset = Offset(x = 100f, y = y),
                zoomLevel = 100f,
                scrollOffsetPx = 1_000_000_000f,
                laneHeightPx = 100f,
                target = target,
            ),
        )
        assertNull(
            hitAutomationPoint(
                points = listOf(point),
                tapOffset = Offset(x = 128f, y = y),
                zoomLevel = 100f,
                scrollOffsetPx = 1_000_000_000f,
                laneHeightPx = 100f,
                target = target,
            )
        )
    }

    @Test
    fun pointerSnappingDoesNotJumpToTheNextMillisecondBecauseOfLargeScroll() {
        val viewport = EditorViewportState(zoomX = 100f, scrollX = 1_000_000_000f)
        listOf(false, true).forEach { snap ->
            assertEquals(
                10_000_000L,
                computeSnappedTimeFromViewport(
                    screenX = 49f,
                    viewport = viewport,
                    bpm = 120.0,
                    gridType = GridUtils.GridType.Flexible.Medium,
                    snapEnabled = snap,
                ),
            )
            assertEquals(
                10_000_001L,
                computeSnappedTimeFromViewport(
                    screenX = 51f,
                    viewport = viewport,
                    bpm = 120.0,
                    gridType = GridUtils.GridType.Flexible.Medium,
                    snapEnabled = snap,
                ),
            )
        }
    }

    @Test
    fun sampleAccurateAudioSplitsShareOneEdgeAtEveryZoom() {
        listOf(0.32f, 1.25f, 5f, 100f).forEach { zoom ->
            val left = projectTimelineSpanPx(startTimeMs = 0.0, endTimeMs = 1000.125, zoomX = zoom)
            val right = projectTimelineSpanPx(startTimeMs = 1000.125, endTimeMs = 2000.0, zoomX = zoom)
            assertEquals(left.endPx, right.startPx)
        }
    }

    @Test
    fun narrowAudioHeadersRemainClickableAfterLongScroll() {
        val entry = AudioEntry(
            startTimeMs = 10_000_001L,
            durationMs = 1L,
            fileName = "alignment.wav",
            clipEndSample = 1L,
            sampleRate = 48000,
            startTimeUs = 10_000_001_000L,
            durationUs = 20L,
        )
        val track = AudioTimelineTrack().apply { entries[entry.startTimeMs] = entry }
        assertEquals(
            entry.startTimeMs,
            findHeaderEntryHit(track = track, x = 1_000_000_100.5, y = 5f, zoom = 100f, headerHeightPx = 20f),
        )
    }
}
