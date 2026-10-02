package dev.anthonyhfm.amethyst.timeline

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.timeline.contract.GridResolution
import dev.anthonyhfm.amethyst.timeline.data.MidiNote
import dev.anthonyhfm.amethyst.timeline.utils.computeVisibleClipWindowPx
import dev.anthonyhfm.amethyst.timeline.utils.projectTimelineSpanPx
import dev.anthonyhfm.amethyst.timeline.viewport.EditorViewportState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PianoRollNoteLayoutTest {
    @Test
    fun zoomingPastTheViewportWidthKeepsTheNoteEndOnTheGrid() {
        listOf(1.5f, 2f, 2.25f, 5f, 20f).forEach { zoom ->
            val viewport = EditorViewportState(
                zoomX = zoom,
                scrollX = 1500f * zoom - 600f,
                viewportWidth = 800f,
            )
            val layout = assertNotNull(
                resolvePianoRollNoteLayout(
                    screenStartPx = viewport.projectTimeToScreenX(timeMs = 0.0),
                    screenEndPx = viewport.projectTimeToScreenX(timeMs = 1500.0),
                    viewportWidthPx = viewport.viewportWidth,
                )
            )
            assertEquals(viewport.timeMsToScreenX(timeMs = 1500.0), layout.leftPx + layout.widthPx)
            assertEquals(0f, layout.leftPx)
            assertEquals(600f, layout.widthPx)
            assertFalse(layout.isLeftEdgeVisible)
            assertTrue(layout.isRightEdgeVisible)
        }
    }

    @Test
    fun aNoteSpanningTheWholeViewportCannotCoverTheLegendOrExposeFalseResizeEdges() {
        val layout = assertNotNull(
            resolvePianoRollNoteLayout(screenStartPx = -2000.0, screenEndPx = 3000.0, viewportWidthPx = 800f)
        )
        assertEquals(0f, layout.leftPx)
        assertEquals(800f, layout.widthPx)
        assertFalse(layout.isLeftEdgeVisible)
        assertFalse(layout.isRightEdgeVisible)
    }

    @Test
    fun croppedGradientsKeepTheirOriginalPhaseAndStopPositions() {
        val layout = assertNotNull(
            resolvePianoRollNoteLayout(screenStartPx = -500.0, screenEndPx = 1500.0, viewportWidthPx = 1000f)
        )
        assertEquals(0.25f, layout.gradientPositionAt(xPx = 0f))
        assertEquals(0.75f, layout.gradientPositionAt(xPx = 1000f))
        assertEquals(500f, layout.gradientStopX(position = 0.5f))
        assertEquals(-500f, layout.gradientStopX(position = 0f))
        assertEquals(1500f, layout.gradientStopX(position = 1f))
    }

    @Test
    fun offscreenNotesAreCulledAndShortVisibleNotesKeepTheirMinimumWidth() {
        assertNull(resolvePianoRollNoteLayout(screenStartPx = -500.0, screenEndPx = -10.0, viewportWidthPx = 800f))
        assertNull(resolvePianoRollNoteLayout(screenStartPx = 850.0, screenEndPx = 900.0, viewportWidthPx = 800f))
        val short = assertNotNull(
            resolvePianoRollNoteLayout(screenStartPx = 25.0, screenEndPx = 26.0, viewportWidthPx = 800f)
        )
        assertEquals(25f, short.leftPx)
        assertEquals(6f, short.widthPx)
    }

    @Test
    fun wideNotesHaveTheSameVisibleEdgesAsTimelineClips() {
        val viewport = EditorViewportState(zoomX = 100f, scrollX = 149700f, viewportWidth = 800f)
        val projected = projectTimelineSpanPx(startTimeMs = 0.0, endTimeMs = 1500.0, zoomX = viewport.zoomX)
        val timelineWindow = assertNotNull(
            computeVisibleClipWindowPx(contentStartPx = projected.startPx, contentEndPx = projected.endPx, viewport = viewport)
        )
        val pianoWindow = assertNotNull(
            resolvePianoRollNoteLayout(
                screenStartPx = viewport.projectTimeToScreenX(timeMs = 0.0),
                screenEndPx = viewport.projectTimeToScreenX(timeMs = 1500.0),
                viewportWidthPx = viewport.viewportWidth,
            )
        )
        assertEquals(timelineWindow.visibleLeftPx.toFloat(), pianoWindow.leftPx)
        assertEquals(timelineWindow.visibleWidthPx.toFloat(), pianoWindow.widthPx)
        assertEquals(timelineWindow.isLeftEdgeVisible, pianoWindow.isLeftEdgeVisible)
        assertEquals(timelineWindow.isRightEdgeVisible, pianoWindow.isRightEdgeVisible)
    }

    @Test
    fun drawingAndHitTestingShareTheSameEdgesAtHighZoomAndScroll() {
        val viewport = EditorViewportState(zoomX = 100f, scrollX = 1_000_000_000f, viewportWidth = 800f)
        val note = MidiNote.withColor(device = 0, pitch = 99, color = Color.Red, startTimeMs = 10_000_001L, durationMs = 5L)
        val metrics = PianoRollMetrics(
            totalPitches = 100,
            noteHeightDp = 22.dp,
            zoomX = viewport.zoomX,
            density = Density(density = 2f),
            gridResolution = GridResolution.OneTwentyEighth,
        )
        val rect = buildNoteRectsScreenSpace(notes = listOf(note), metrics = metrics, viewport = viewport).single()
        val layout = assertNotNull(
            resolvePianoRollNoteLayout(
                screenStartPx = viewport.projectTimeToScreenX(timeMs = note.startTimeMs.toDouble()),
                screenEndPx = viewport.projectTimeToScreenX(timeMs = note.endTimeMs.toDouble()),
                viewportWidthPx = viewport.viewportWidth,
            )
        )
        assertEquals(100f, layout.leftPx)
        assertEquals(600f, layout.leftPx + layout.widthPx)
        assertEquals(rect.left, layout.leftPx)
        assertEquals(rect.right, layout.leftPx + layout.widthPx)
        assertTrue(rect.contains(point = Offset(x = 599f, y = 10f)))
    }

    @Test
    fun extremelyWideNotesKeepTheirVisibleEndPrecise() {
        val viewport = EditorViewportState(zoomX = 100f, scrollX = 1_000_000_000f, viewportWidth = 800f)
        val layout = assertNotNull(
            resolvePianoRollNoteLayout(
                screenStartPx = viewport.projectTimeToScreenX(timeMs = 0.0),
                screenEndPx = viewport.projectTimeToScreenX(timeMs = 10_000_001.0),
                viewportWidthPx = viewport.viewportWidth,
            )
        )
        assertEquals(100f, layout.widthPx)
        assertTrue(layout.isRightEdgeVisible)
    }
}
