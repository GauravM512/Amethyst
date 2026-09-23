package dev.anthonyhfm.amethyst.core.loading

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.RUSAGE_SELF
import platform.posix.getrusage
import platform.posix.rusage

@OptIn(ExperimentalForeignApi::class)
actual fun residentMemoryBytes(): Long? = memScoped {
    val usage = alloc<rusage>()
    if (getrusage(RUSAGE_SELF, usage.ptr) == 0) usage.ru_maxrss.toLong() else null
}
