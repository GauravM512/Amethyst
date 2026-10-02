package dev.anthonyhfm.amethyst.core.util

import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual object ConversionTempFiles {
    actual fun newPath(extension: String): String =
        "${NSTemporaryDirectory()}amethyst-entry-${NSUUID().UUIDString}.${extension.take(8)}"

    actual fun remove(path: String) {
        NSFileManager.defaultManager.removeItemAtPath(path, null)
    }
}
