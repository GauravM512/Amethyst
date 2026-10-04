package dev.anthonyhfm.amethyst.home.data

import dev.anthonyhfm.amethyst.core.util.MobileFileStorage
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/** Only app-owned project files may be deleted from the Projects tab. */
internal object AndroidLocalProjectDeletion {
    private val extensions = setOf("ame", "als", "zip", "rar", "approj")

    fun targetFor(path: String): File? {
        val amethyst = MobileFileStorage.getAmethystDirectory().canonicalFile
        val file = File(path).canonicalFile
        if (file.extension.lowercase() !in extensions) return null

        if (file.parentFile == amethyst) return file

        val original = file.parentFile ?: return null
        if (original.name != "Original") return null
        val project = original.parentFile ?: return null
        if (project.parentFile != File(amethyst, "Hub").canonicalFile &&
            project.parentFile != File(amethyst, "Projects").canonicalFile
        ) return null
        return project
    }

    fun delete(path: String): Boolean {
        val target = targetFor(path) ?: return false
        if (!target.exists()) return false
        val deleted = runCatching {
            Files.walkFileTree(target.toPath(), object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: java.io.IOException?): FileVisitResult {
                    if (exc != null) throw exc
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }
            })
        }.isSuccess
        if (!deleted) return false
        return true
    }
}
