package dev.anthonyhfm.amethyst.home.data

import dev.anthonyhfm.amethyst.core.util.MobileFileStorage
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Keeps picked originals beside their converted cache, matching the iOS project layout. */
internal object AndroidProjectImporter {
    data class Imported(val id: String, val file: PlatformFile, val sha256: String)

    suspend fun importOriginal(source: PlatformFile): Imported = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val root = File(MobileFileStorage.getAmethystDirectory(), "Projects/$id")
        val originals = File(root, "Original")
        check(originals.mkdirs()) { "Could not create project directory" }
        try {
            val filename = source.name.substringAfterLast('/').substringAfterLast('\\')
            require(filename.isNotBlank() && filename !in setOf(".", ".."))
            val target = File(originals, filename)
            val staging = File(originals, ".${UUID.randomUUID()}.part")
            val digest = MessageDigest.getInstance("SHA-256")
            val input = when (val androidFile = source.androidFile) {
                is AndroidFile.FileWrapper -> androidFile.file.inputStream()
                is AndroidFile.UriWrapper -> FileKit.context.contentResolver.openInputStream(androidFile.uri)
                    ?: error("Could not open selected file")
            }
            input.use { stream ->
                staging.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                    }
                }
            }
            check(staging.renameTo(target)) { "Could not save selected file" }
            val sha256 = digest.digest()
                .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
            Imported(id, PlatformFile(target.absolutePath), sha256)
        } catch (error: Exception) {
            root.deleteRecursively()
            throw error
        }
    }
}
