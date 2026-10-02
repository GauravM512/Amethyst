package dev.anthonyhfm.amethyst.core.engine.echo

import kotlinx.atomicfu.atomic

internal class AdaptiveAudioBufferingPolicy(
    private val periodFrames: Int,
    ringCapacityFrames: Int,
) {
    init {
        require(periodFrames > 0)
    }

    private val maximumQueuedPeriods = ringCapacityFrames / periodFrames
    private val queuedPeriods = atomic(AudioOutputBufferingPolicy.TARGET_QUEUED_PERIODS)

    init {
        require(maximumQueuedPeriods >= AudioOutputBufferingPolicy.TARGET_QUEUED_PERIODS)
    }

    val targetQueuedFrames: Int
        get() = queuedPeriods.value * periodFrames

    fun recordUnderruns(underrunCount: Long) {
        if (underrunCount > 0L && queuedPeriods.value < maximumQueuedPeriods) {
            queuedPeriods.incrementAndGet()
        }
    }
}
