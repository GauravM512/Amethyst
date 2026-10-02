package dev.anthonyhfm.amethyst.core.util

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.path
import java.io.File

actual object ConversionTempFiles {
    actual fun newPath(extension: String): String =
        File.createTempFile("amethyst-entry-", ".${extension.take(8)}", File(FileKit.cacheDir.path)).absolutePath

    actual fun remove(path: String) {
        File(path).delete()
    }
}
