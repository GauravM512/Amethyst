package dev.anthonyhfm.amethyst.core.engine.audio.source

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal actual object PreparedAudioDiskCache {
    actual fun read(root: String, key: String, expectedBytes: Int): ByteArray? {
        val file = File(root, "$key.pcm")
        if (!file.isFile || file.length() != expectedBytes.toLong()) return null
        return file.readBytes()
    }

    actual fun write(root: String, key: String, bytes: ByteArray) {
        val directory = File(root).apply { mkdirs() }
        val target = File(directory, "$key.pcm")
        val staging = File.createTempFile("prepared-", ".part", directory)
        try {
            staging.outputStream().buffered().use { output -> output.write(bytes) }
            try {
                Files.move(staging.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: Exception) {
                Files.move(staging.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            staging.delete()
        }
    }
}
