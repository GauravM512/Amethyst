package dev.anthonyhfm.amethyst.timeline.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals

class TimelineAutomationLaneTest {
    @Test
    fun valueAtInterpolatesInDisplayDomainAndHoldsFallbackBeforeFirstPoint() {
        val target = TimelineTrackAutomationTarget.VOLUME
        val lane = TimelineAutomationLane(
            target = target,
            points = listOf(
                TimelineAutomationPoint(
                    timeMs = 1_000L,
                    value = target.displayValueToValue(-12f)
                ),
                TimelineAutomationPoint(
                    timeMs = 3_000L,
                    value = target.displayValueToValue(12f)
                )
            )
        )

        assertEquals(0.42f, lane.valueAt(timeMs = 0L, defaultValue = 0.42f), 0.0001f)
        assertEquals(target.displayValueToValue(-12f), lane.valueAt(timeMs = 1_000L, defaultValue = 0.42f), 0.0001f)
        assertEquals(1f, lane.valueAt(timeMs = 2_000L, defaultValue = 0.42f), 0.0001f)
        assertEquals(target.displayValueToValue(12f), lane.valueAt(timeMs = 3_000L, defaultValue = 0.42f), 0.0001f)
        assertEquals(target.displayValueToValue(12f), lane.valueAt(timeMs = 4_000L, defaultValue = 0.42f), 0.0001f)
    }

    @Test
    fun normalizedCleansLegacyBindingsAndDuplicateTimes() {
        val lane = TimelineAutomationLane(
            target = TimelineTrackAutomationTarget.VOLUME,
            bindingId = "legacy-binding",
            points = listOf(
                TimelineAutomationPoint(timeMs = -25L, value = 1.5f, pointId = ""),
                TimelineAutomationPoint(timeMs = 500L, value = 0.25f, pointId = "first"),
                TimelineAutomationPoint(timeMs = 500L, value = 0.75f, pointId = "last")
            )
        ).normalized()

        assertNull(lane.bindingId)
        assertEquals(2, lane.points.size)
        assertEquals(0L, lane.points.first().timeMs)
        assertEquals(1.5f, lane.points.first().value, 0.0001f)
        assertEquals("last", lane.points.last().pointId)
        assertEquals(0.75f, lane.points.last().value, 0.0001f)
        assertTrue(lane.points.first().pointId.isNotBlank())
    }

    @Test
    fun disabledLaneFallsBackToProvidedDefaultValue() {
        val lane = TimelineAutomationLane(
            target = TimelineTrackAutomationTarget.VOLUME,
            enabled = false,
            points = listOf(
                TimelineAutomationPoint(timeMs = 0L, value = 0.1f)
            )
        )

        assertEquals(0.42f, lane.valueAt(timeMs = 2_000L, defaultValue = 0.42f), 0.0001f)
    }

    @Test
    fun valueAtAppliesCurveStoredOnSegmentStartPoint() {
        val target = TimelineTrackAutomationTarget.VOLUME
        val curvedLane = TimelineAutomationLane(
            target = target,
            points = listOf(
                TimelineAutomationPoint(timeMs = 0L, value = 0f, curve = 0.75f, pointId = "start"),
                TimelineAutomationPoint(timeMs = 1_000L, value = 1f, pointId = "end")
            )
        )
        val linearLane = TimelineAutomationLane(
            target = target,
            points = listOf(
                TimelineAutomationPoint(timeMs = 0L, value = 0f, pointId = "linear-start"),
                TimelineAutomationPoint(timeMs = 1_000L, value = 1f, pointId = "linear-end")
            )
        )

        val midpoint = curvedLane.valueAt(timeMs = 500L, defaultValue = 0f)
        val linearMidpoint = linearLane.valueAt(timeMs = 500L, defaultValue = 0f)

        assertNotEquals(linearMidpoint, midpoint, 0.0001f)
        assertTrue(midpoint > linearMidpoint)
    }

    @Test
    fun valueAtPassesExactlyThroughExplicitCurveHandle() {
        val curvedLane = TimelineAutomationLane(
            target = TimelineTrackAutomationTarget.VOLUME,
            points = listOf(
                TimelineAutomationPoint(
                    timeMs = 0L,
                    value = 0f,
                    curveHandleTime = 0.25f,
                    curveHandleValue = 0.75f,
                    pointId = "start"
                ),
                TimelineAutomationPoint(timeMs = 1_000L, value = 1f, pointId = "end")
            )
        )

        assertEquals(0.75f, curvedLane.valueAt(timeMs = 250L, defaultValue = 0f), 0.0001f)
    }

    @Test
    fun explicitCurveHandlesStaySymmetricForMirroredHandleValues() {
        val target = TimelineTrackAutomationTarget.VOLUME
        val upperLane = TimelineAutomationLane(
            target = target,
            points = listOf(
                TimelineAutomationPoint(
                    timeMs = 0L,
                    value = target.displayValueToValue(-12f),
                    curveHandleTime = 0.5f,
                    curveHandleValue = target.displayValueToValue(6f),
                    pointId = "upper-start"
                ),
                TimelineAutomationPoint(
                    timeMs = 1_000L,
                    value = target.displayValueToValue(12f),
                    pointId = "upper-end"
                )
            )
        )
        val lowerLane = TimelineAutomationLane(
            target = target,
            points = listOf(
                TimelineAutomationPoint(
                    timeMs = 0L,
                    value = target.displayValueToValue(-12f),
                    curveHandleTime = 0.5f,
                    curveHandleValue = target.displayValueToValue(-6f),
                    pointId = "lower-start"
                ),
                TimelineAutomationPoint(
                    timeMs = 1_000L,
                    value = target.displayValueToValue(12f),
                    pointId = "lower-end"
                )
            )
        )
        val linearLane = TimelineAutomationLane(
            target = target,
            points = listOf(
                TimelineAutomationPoint(
                    timeMs = 0L,
                    value = target.displayValueToValue(-12f),
                    pointId = "linear-start"
                ),
                TimelineAutomationPoint(
                    timeMs = 1_000L,
                    value = target.displayValueToValue(12f),
                    pointId = "linear-end"
                )
            )
        )

        assertEquals(
            target.valueToDisplayValue(linearLane.valueAt(timeMs = 250L, defaultValue = 0f)) * 2f,
            target.valueToDisplayValue(upperLane.valueAt(timeMs = 250L, defaultValue = 0f)) +
                target.valueToDisplayValue(lowerLane.valueAt(timeMs = 250L, defaultValue = 0f)),
            0.0001f
        )
        assertEquals(
            target.valueToDisplayValue(linearLane.valueAt(timeMs = 750L, defaultValue = 0f)) * 2f,
            target.valueToDisplayValue(upperLane.valueAt(timeMs = 750L, defaultValue = 0f)) +
                target.valueToDisplayValue(lowerLane.valueAt(timeMs = 750L, defaultValue = 0f)),
            0.0001f
        )
    }

    @Test
    fun clippedRangeRebuildsHandleWithoutOffset() {
        val sourceLane = TimelineAutomationLane(
            target = TimelineTrackAutomationTarget.VOLUME,
            points = listOf(
                TimelineAutomationPoint(
                    timeMs = 0L,
                    value = 0f,
                    curveHandleTime = 0.5f,
                    curveHandleValue = 0.5f,
                    pointId = "start"
                ),
                TimelineAutomationPoint(timeMs = 1_000L, value = 1f, pointId = "end")
            )
        )

        val clippedLane = sourceLane.clippedToRange(
            startMs = 200L,
            endMs = 800L,
            baseValue = 0f
        )
        assertNotNull(clippedLane)

        assertEquals(
            sourceLane.valueAt(timeMs = 350L, defaultValue = 0f),
            clippedLane.valueAt(timeMs = 150L, defaultValue = 0f),
            0.0001f
        )
        assertEquals(
            sourceLane.valueAt(timeMs = 500L, defaultValue = 0f),
            clippedLane.valueAt(timeMs = 300L, defaultValue = 0f),
            0.0001f
        )
    }

    @Test
    fun volumeAutomationMapsUnityGainToCenteredDisplayValue() {
        assertEquals(0.5f, TimelineTrackAutomationTarget.VOLUME.valueToDisplayProgress(1f), 0.0001f)
        assertEquals(1f, TimelineTrackAutomationTarget.VOLUME.displayProgressToValue(0.5f), 0.0001f)
        assertEquals("0 dB", TimelineTrackAutomationTarget.VOLUME.formatValue(1f))
    }

    @Test
    fun volumeAutomationSnapsNearZeroDbBackToUnityGain() {
        assertEquals(0f, TimelineTrackAutomationTarget.VOLUME.snapDisplayValue(0.2f), 0.0001f)
        assertEquals(1f, TimelineTrackAutomationTarget.VOLUME.snapValue(1.02f), 0.0001f)
    }

    @Test
    fun defaultCurveHandleUsesDisplayDomainMidpointForVolume() {
        val target = TimelineTrackAutomationTarget.VOLUME
        val handle = TimelineAutomationPoint(
            timeMs = 0L,
            value = target.displayValueToValue(-12f),
            pointId = "start"
        ).displayCurveHandle(
            target = target,
            endPoint = TimelineAutomationPoint(
                timeMs = 1_000L,
                value = target.displayValueToValue(12f),
                pointId = "end"
            )
        )

        assertEquals(0f, target.valueToDisplayValue(handle.value), 0.0001f)
    }

    @Test
    fun withCurveHandleClampsStoredHandleToEndpointRange() {
        val target = TimelineTrackAutomationTarget.VOLUME
        val startPoint = TimelineAutomationPoint(
            timeMs = 0L,
            value = target.displayValueToValue(-12f),
            pointId = "start"
        )
        val endPoint = TimelineAutomationPoint(
            timeMs = 1_000L,
            value = target.displayValueToValue(0f),
            pointId = "end"
        )

        val updatedPoint = startPoint.withCurveHandle(
            target = target,
            endPoint = endPoint,
            timeProgress = 0.5f,
            value = target.displayValueToValue(12f)
        )
        val storedHandleValue = updatedPoint.curveHandleValue

        assertNotNull(storedHandleValue)
        assertEquals(0f, target.valueToDisplayValue(storedHandleValue), 0.0001f)
    }

    @Test
    fun quadraticCurveDoesNotOvershootEndpointRange() {
        val target = TimelineTrackAutomationTarget.VOLUME
        val lane = TimelineAutomationLane(
            target = target,
            points = listOf(
                TimelineAutomationPoint(
                    timeMs = 0L,
                    value = target.displayValueToValue(-6f),
                    curveHandleTime = 0.1f,
                    curveHandleValue = target.displayValueToValue(6f),
                    pointId = "start"
                ),
                TimelineAutomationPoint(
                    timeMs = 1_000L,
                    value = target.displayValueToValue(6f),
                    pointId = "end"
                )
            )
        )

        for (timeMs in 0L..1_000L step 25L) {
            val displayValue = target.valueToDisplayValue(
                lane.valueAt(timeMs = timeMs, defaultValue = target.defaultValue)
            )
            assertTrue(displayValue in -6.0001f..6.0001f)
        }
    }

    @Test
    fun normalizedKeepsExpandedCurveRangeUsedByUi() {
        val point = TimelineAutomationPoint(
            timeMs = 0L,
            value = 1f,
            curve = 2.5f
        ).normalized(TimelineTrackAutomationTarget.VOLUME)

        assertEquals(2.5f, point.curve, 0.0001f)
    }
}
