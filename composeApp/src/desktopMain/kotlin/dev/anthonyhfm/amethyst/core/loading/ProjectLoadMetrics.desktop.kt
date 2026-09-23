package dev.anthonyhfm.amethyst.core.loading

actual fun residentMemoryBytes(): Long? = Runtime.getRuntime().run { totalMemory() - freeMemory() }
