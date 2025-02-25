package dev.anthonyhfm.amethyst.core.util

import kotlin.time.Duration
import kotlin.time.TimeSource

class StopWatch {
    private var startMark = TimeSource.Monotonic.markNow()

    val frequency: Long = 1_000_000_000L

    fun reset() {
        startMark = TimeSource.Monotonic.markNow()
    }

    fun elapsedMillis(): Double {
        return startMark.elapsedNow().inWholeMilliseconds.toDouble()
    }

    fun elapsedNanos(): Long {
        return startMark.elapsedNow().inWholeNanoseconds
    }

    fun elapsedDuration(): Duration {
        return startMark.elapsedNow()
    }

    fun elapsedTicks(): Long {
        return (elapsedNanos() * frequency) / 1_000_000_000L
    }
}