package dev.anthonyhfm.amethyst.timeline

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.timeline.contract.GridResolution
import kotlin.test.Test
import kotlin.test.assertEquals

class PianoRollPlaybackTest {
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
