package dev.anthonyhfm.amethyst.core.util

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absoluteFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipInputStream

actual object Zip {
    actual fun getEntries(
        file: PlatformFile,
    ): List<ZipEntry> {
        val data: ByteArray = runBlocking(Dispatchers.IO) {
            file.readBytes()
        }

        return try {
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
            entries
        } catch (e: Exception) {
            println("Error reading ZIP file: ${e.message}")
            emptyList()
        }
    }

    actual fun getPaths(file: PlatformFile): List<String> {
        val data: ByteArray = runBlocking(Dispatchers.IO) {
            file.readBytes()
        }

        return try {
            val zipFile = ZipInputStream(data.inputStream())
            val entries = mutableListOf<String>()

            var entry = zipFile.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                entry = zipFile.nextEntry
            }

            zipFile.close()
            entries
        } catch (e: Exception) {
            println("Error reading ZIP paths: ${e.message}")
            emptyList()
        }
    }

    actual fun decode(data: ByteArray): ByteArray {
        if (data.size < 4) return data

        val b0 = data[0].toUByte().toInt()
        val b1 = data[1].toUByte().toInt()

        // Check for GZIP header (0x1F 0x8B)
        if (b0 == 0x1F && b1 == 0x8B) {
            return try {
                GZIPInputStream(data.inputStream()).use { it.readBytes() }
            } catch (e: Exception) {
                println("GZIP decompression failed: ${e.message}")
                data
            }
        }

        // Check for ZIP header (PK\u0003\u0004 -> 0x50 0x4B 0x03 0x04)
        if (b0 == 0x50 && b1 == 0x4B && data[2].toInt() == 0x03 && data[3].toInt() == 0x04) {
            return try {
                ZipInputStream(data.inputStream()).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.endsWith(".als")) {
                            val entryData = zipStream.readBytes()
                            // Recursively decode to handle GZIP inside ZIP
                            return decode(entryData)
                        }
                        entry = zipStream.nextEntry
                    }
                }
                data
            } catch (e: Exception) {
                println("ZIP extraction failed: ${e.message}")
                data
            }
        }

        return data
    }

    actual fun encode(data: ByteArray): ByteArray {
        ByteArrayOutputStream(data.size).use { out ->
            GZIPOutputStream(out).use { gzip ->
                gzip.write(data)
            }

            return out.toByteArray()
        }
    }
}