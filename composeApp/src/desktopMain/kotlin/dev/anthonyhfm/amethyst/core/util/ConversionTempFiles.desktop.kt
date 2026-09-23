package dev.anthonyhfm.amethyst.core.util

import java.io.File

actual object ConversionTempFiles {
    actual fun newPath(extension: String): String =
        File.createTempFile("amethyst-entry-", ".${extension.take(8)}").absolutePath

    actual fun remove(path: String) {
        File(path).delete()
    }
}
