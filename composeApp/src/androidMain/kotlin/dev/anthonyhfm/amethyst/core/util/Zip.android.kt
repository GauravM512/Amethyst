package dev.anthonyhfm.amethyst.core.util

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absoluteFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

actual object Zip {
    actual fun getEntries(
        file: PlatformFile,
    ): List<ZipEntry> {
        val data: ByteArray = runBlocking(Dispatchers.IO) {
            file.readBytes()
        }

        val zipFile = ZipInputStream(data.inputStream())

        val entries = mutableListOf<ZipEntry>()

        var entry = zipFile.nextEntry
        while (entry != null) {
            entries.add(
                ZipEntry(
                    path = entry.name,
                    data = zipFile.readBytes(),
                    isDirectory = entry.isDirectory,
                )
            )
            entry = zipFile.nextEntry
        }

        zipFile.close()

        return entries
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    actual fun decode(file: String): ByteArray {
        val _file = File(file).let {
            if (!it.exists() || !it.isFile) {
                return ByteArray(0)
            }

            return@let it.inputStream()
        }

        val zipFile = GZIPInputStream(_file)

        return zipFile.readAllBytes()
    }
}