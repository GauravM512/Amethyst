package dev.anthonyhfm.amethyst.timeline

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.timeline.contract.GridResolution
import dev.anthonyhfm.amethyst.timeline.viewport.EditorViewportState
import kotlin.test.Test
import kotlin.test.assertEquals

class PianoRollPlaybackTest {
    private val viewport = EditorViewportState(
        zoomX = 1f,
        scrollX = 0f,
        viewportWidth = 1000f,
        contentWidth = 5000f,
    )

    @Test
    fun followingKeepsTheViewportStableWhileThePlayheadIsVisible() {
        assertEquals(viewport, followPianoRollPlayhead(viewport = viewport, positionMs = 850L))
    }

    @Test
    fun followingPagesForwardBeforeThePlayheadLeavesTheView() {
        val next = followPianoRollPlayhead(viewport = viewport, positionMs = 950L)
        assertEquals(850f, next.scrollX)
        assertEquals(100f, next.timeMsToScreenX(timeMs = 950.0))
    }

    @Test
    fun followingReturnsToTheBeginningAfterPlaybackRestarts() {
        assertEquals(0f, followPianoRollPlayhead(viewport = viewport.copy(scrollX = 2000f), positionMs = 0L).scrollX)
    }

    @Test
    fun followingClampsAtTheContentEndAndWaitsForLayout() {
        assertEquals(4000f, followPianoRollPlayhead(viewport = viewport, positionMs = 4990L).scrollX)
        val unmeasured = viewport.copy(viewportWidth = 0f)
        assertEquals(unmeasured, followPianoRollPlayhead(viewport = unmeasured, positionMs = 950L))
    }

    @Test
    fun foldedPadsPreserveTheirActualAddressesForDrawingAndHitTesting() {
        val metrics = PianoRollMetrics(
            totalPitches = 3,
            noteHeightDp = 22.dp,
            zoomX = 1f,
            density = Density(density = 2f),
            gridResolution = GridResolution.Quarter,
            pitches = listOf(11, 45, 88),
        )
        assertEquals(0f, metrics.pitchToYPx(pitch = 88))
        assertEquals(44f, metrics.pitchToYPx(pitch = 45))
        assertEquals(88f, metrics.pitchToYPx(pitch = 11))
        assertEquals(88, metrics.yPxToPitch(y = 10f))
        assertEquals(45, metrics.yPxToPitch(y = 60f))
        assertEquals(11, metrics.yPxToPitch(y = 120f))
        assertEquals(132f, metrics.canvasHeightPx)
    }
}
