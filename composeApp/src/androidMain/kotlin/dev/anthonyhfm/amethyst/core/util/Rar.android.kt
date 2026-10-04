package dev.anthonyhfm.amethyst.core.util

import com.github.junrar.Archive
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.path
import java.io.ByteArrayOutputStream
import java.io.File

actual object Rar {
    actual fun getEntries(file: PlatformFile): List<ZipEntry> = readArchive(file = file) { archive ->
        archive.fileHeaders.map { header ->
            val data = if (header.isDirectory) {
                ByteArray(size = 0)
            } else {
                ByteArrayOutputStream().use { output ->
                    archive.extractFile(header, output)
                    output.toByteArray()
                }
            }

            ZipEntry(
                path = header.fileName,
                data = data,
                isDirectory = header.isDirectory,
            )
        }
    }

    actual fun getPaths(file: PlatformFile): List<String> = readArchive(file = file) { archive ->
        archive.fileHeaders.map { header -> header.fileName }
    }

    private fun <T> readArchive(
        file: PlatformFile,
        read: (Archive) -> List<T>,
    ): List<T> {
        var ownedTempFile: File? = null

        return try {
            val archiveFile = when (val androidFile = file.androidFile) {
                is AndroidFile.FileWrapper -> androidFile.file
                is AndroidFile.UriWrapper -> {
                    File.createTempFile("amethyst-rar-", ".rar", File(FileKit.cacheDir.path)).also { temp ->
                        ownedTempFile = temp
                        val input = FileKit.context.contentResolver.openInputStream(androidFile.uri)
                            ?: error("Could not open RAR content URI")

                        input.use { source ->
                            temp.outputStream().use { output ->
                                source.copyTo(out = output, bufferSize = 64 * 1024)
                            }
                        }
                    }
                }
            }

            if (!archiveFile.isFile) {
                return emptyList()
            }

            Archive(archiveFile).use(block = read)
        } catch (exception: Exception) {
            println("Error reading RAR file: ${exception.message}")
            emptyList()
        } finally {
            ownedTempFile?.delete()
        }
    }
}
