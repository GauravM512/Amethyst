package dev.anthonyhfm.amethyst.core.loading

import kotlin.time.TimeSource

/** Lightweight stage measurements for comparing identical imports on device. */
object ProjectLoadMetrics {
    inline fun <T> measure(stage: String, block: () -> T): T {
        val started = TimeSource.Monotonic.markNow()
        val before = residentMemoryBytes()
        try {
            return block()
        } finally {
            val after = residentMemoryBytes()
            println("ProjectLoad stage=$stage durationMs=${started.elapsedNow().inWholeMilliseconds} memoryBefore=$before memoryAfter=$after")
        }
    }

    suspend inline fun <T> measureSuspend(stage: String, crossinline block: suspend () -> T): T {
        val started = TimeSource.Monotonic.markNow()
        val before = residentMemoryBytes()
        try {
            return block()
        } finally {
            val after = residentMemoryBytes()
            println("ProjectLoad stage=$stage durationMs=${started.elapsedNow().inWholeMilliseconds} memoryBefore=$before memoryAfter=$after")
        }
    }
}

/** iOS uses resident high-water, Android uses current proportional set size. */
expect fun residentMemoryBytes(): Long?
