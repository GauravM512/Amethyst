package dev.anthonyhfm.amethyst.core.util

import kotlin.time.Duration
import kotlin.time.TimeSource

class StopWatch {
    private var startMark = TimeSource.Monotonic.markNow()
    private var lastTicks: Long = 0L
    private var lastNanos: Long = 0L

    val frequency: Long = 10_000_000L // 10 Millionen Ticks pro Sekunde

    fun reset() {
        startMark = TimeSource.Monotonic.markNow()
        lastTicks = 0L
        lastNanos = 0L
    }

    fun elapsedMillis(): Double {
        return startMark.elapsedNow().inWholeMilliseconds.toDouble()
    }

    fun elapsedNanos(): Long {
        return startMark.elapsedNow().inWholeNanoseconds.coerceAtLeast(0)
    }

    fun elapsedTicks(): Long {
        val nanos = elapsedNanos()
        val deltaNanos = (nanos - lastNanos).coerceAtLeast(0)

        val deltaTicks = deltaNanos / 100
        val currentTicks = lastTicks + deltaTicks

        lastTicks = currentTicks
        lastNanos = nanos

        return currentTicks
    }
}
