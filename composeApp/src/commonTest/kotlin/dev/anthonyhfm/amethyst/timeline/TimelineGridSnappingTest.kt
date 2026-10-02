package dev.anthonyhfm.amethyst.timeline

import dev.anthonyhfm.amethyst.timeline.utils.GridUtils
import dev.anthonyhfm.amethyst.timeline.utils.computeSnappedTimeFromContentX
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimelineGridSnappingTest {
    private val flexibleGrids = listOf(
        GridUtils.GridType.Flexible.Smallest,
        GridUtils.GridType.Flexible.Small,
        GridUtils.GridType.Flexible.Medium,
        GridUtils.GridType.Flexible.Large,
        GridUtils.GridType.Flexible.Largest,
    )

    @Test
    fun everyFlexibleGridGetsFinerAsTheTimelineZoomsIn() {
        flexibleGrids.forEach { grid ->
            val intervals = listOf(0.025f, 0.25f, 5f, 100f).map { zoom ->
                GridUtils.computeWithGridType(zoomLevel = zoom, bpm = 120.0, gridType = grid).intervalMs
            }
            assertTrue(intervals.zipWithNext().all { (before, after) -> after < before }, "$grid: $intervals")
            assertTrue(intervals.last() in 1L..2L)
        }
    }

    @Test
    fun flexibleSizesControlDensityAtTheSameZoom() {
        val intervals = flexibleGrids.map { grid ->
            GridUtils.computeWithGridType(zoomLevel = 1f, bpm = 120.0, gridType = grid).intervalMs
        }
        assertEquals(listOf(16L, 31L, 62L, 125L, 250L), intervals)
    }

    @Test
    fun deeplyZoomedFlexibleGridSnapsToMilliseconds() {
        assertEquals(
            13L,
            GridUtils.snapToGrid(
                timeMs = 13L,
                zoomLevel = 100f,
                bpm = 120.0,
                gridType = GridUtils.GridType.Flexible.Medium,
            ),
        )
    }

    @Test
    fun zoomingOutExpandsFlexibleGridsBeyondOneBar() {
        flexibleGrids.forEach { grid ->
            val interval = GridUtils.computeWithGridType(zoomLevel = 0.0025f, bpm = 120.0, gridType = grid)
            assertTrue(interval.intervalMs > 2000L)
            assertTrue(interval.majorIntervalMs >= interval.intervalMs)
        }
    }

    @Test
    fun timelinePointerHardSnapsToActiveGrid() {
        assertEquals(
            500L,
            computeSnappedTimeFromContentX(
                x = 260f,
                zoomLevel = 1f,
                bpm = 120.0,
                gridType = GridUtils.GridType.Fixed._1_4,
            ),
        )
    }

    @Test
    fun timelinePointerBypassesGridWhenSnapIsDisabled() {
        assertEquals(
            260L,
            computeSnappedTimeFromContentX(
                x = 260f,
                zoomLevel = 1f,
                bpm = 120.0,
                gridType = GridUtils.GridType.Fixed._1_4,
                snapEnabled = false,
            ),
        )
    }
}
