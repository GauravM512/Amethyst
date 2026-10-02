package dev.anthonyhfm.amethyst.core.util

/** Temporary extraction files used only while decoding an archive audio entry. */
expect object ConversionTempFiles {
    fun newPath(extension: String): String
    fun remove(path: String)
}
