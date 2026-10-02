package dev.anthonyhfm.amethyst.core.engine.echo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdaptiveAudioBufferingPolicyTest {
    @Test
    fun healthyPlaybackKeepsLowLatency() {
        val policy = AdaptiveAudioBufferingPolicy(
            periodFrames = 192,
            ringCapacityFrames = 3_840,
        )

        repeat(times = 20) {
            policy.recordUnderruns(underrunCount = 0L)
        }

        assertEquals(expected = 384, actual = policy.targetQueuedFrames)
    }

    @Test
    fun underrunsIncreaseReserveWithoutExceedingCapacity() {
        val policy = AdaptiveAudioBufferingPolicy(
            periodFrames = 192,
            ringCapacityFrames = 3_840,
        )

        policy.recordUnderruns(underrunCount = 12L)
        assertEquals(expected = 576, actual = policy.targetQueuedFrames)

        repeat(times = 20) {
            policy.recordUnderruns(underrunCount = 1L)
        }

        assertEquals(expected = 3_840, actual = policy.targetQueuedFrames)
    }

    @Test
    fun reserveFitsCompletePeriodsInRing() {
        val policy = AdaptiveAudioBufferingPolicy(
            periodFrames = 256,
            ringCapacityFrames = 1_000,
        )

        repeat(times = 20) {
            policy.recordUnderruns(underrunCount = 1L)
        }

        assertEquals(expected = 768, actual = policy.targetQueuedFrames)
    }

    @Test
    fun rejectsInvalidPeriodAndInsufficientCapacity() {
        assertFailsWith<IllegalArgumentException> {
            AdaptiveAudioBufferingPolicy(
                periodFrames = 0,
                ringCapacityFrames = 1_024,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            AdaptiveAudioBufferingPolicy(
                periodFrames = 256,
                ringCapacityFrames = 511,
            )
        }
    }
}
