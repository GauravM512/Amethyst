package dev.anthonyhfm.amethyst.core.loading

import dev.anthonyhfm.amethyst.core.util.Platform
import dev.anthonyhfm.amethyst.core.util.platform
import kotlin.time.TimeSource

/** Lightweight stage measurements for comparing identical imports on device. */
object ProjectLoadMetrics {
    val memoryMetric: String = when (platform) {
        Platform.Android -> "androidPss"
        Platform.iOS -> "iosPeakResident"
        is Platform.Desktop -> "jvmUsedHeap"
    }

    inline fun <T> measure(stage: String, block: () -> T): T {
        val started = TimeSource.Monotonic.markNow()
        val before = residentMemoryBytes()
        try {
            return block()
        } finally {
            val after = residentMemoryBytes()
            println("ProjectLoad stage=$stage durationMs=${started.elapsedNow().inWholeMilliseconds} memoryMetric=$memoryMetric memoryBefore=$before memoryAfter=$after")
        }
    }

    suspend inline fun <T> measureSuspend(stage: String, crossinline block: suspend () -> T): T {
        val started = TimeSource.Monotonic.markNow()
        val before = residentMemoryBytes()
        try {
            return block()
        } finally {
            val after = residentMemoryBytes()
            println("ProjectLoad stage=$stage durationMs=${started.elapsedNow().inWholeMilliseconds} memoryMetric=$memoryMetric memoryBefore=$before memoryAfter=$after")
        }
    }
}

/** iOS uses resident high-water, Android uses current proportional set size. */
expect fun residentMemoryBytes(): Long?
