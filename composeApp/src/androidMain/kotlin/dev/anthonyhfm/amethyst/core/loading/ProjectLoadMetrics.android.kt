package dev.anthonyhfm.amethyst.core.loading

import android.os.Debug

actual fun residentMemoryBytes(): Long? = Debug.getPss().toLong() * 1024L
