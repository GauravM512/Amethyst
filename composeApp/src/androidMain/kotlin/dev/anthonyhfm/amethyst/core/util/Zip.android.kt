package dev.anthonyhfm.amethyst.core.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

actual object Zip {
    actual fun getEntries(file: PlatformFile): List<ZipEntry> {
        val path = file.path
        if (path.isBlank()) return emptyList()
        val fis = File(path).let {
            if (!it.exists() || !it.isFile) return emptyList()
            FileInputStream(it)
        }
        val zis = ZipInputStream(fis)
        val result = mutableListOf<ZipEntry>()
        var e = zis.nextEntry
        while (e != null) {
            val data = if (!e.isDirectory) zis.readBytes() else ByteArray(0)
            result.add(ZipEntry(path = e.name, data = data, isDirectory = e.isDirectory))
            e = zis.nextEntry
        }
        zis.close()
        return result
    }

    actual fun decode(file: String): ByteArray {
        val fis = File(file).let {
            if (!it.exists() || !it.isFile) return ByteArray(0)
            FileInputStream(it)
        }
        return GZIPInputStream(fis).use { it.readBytes() }
    }
}
