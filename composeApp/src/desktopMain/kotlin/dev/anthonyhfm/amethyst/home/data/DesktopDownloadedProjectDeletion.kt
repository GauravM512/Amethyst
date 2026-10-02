package dev.anthonyhfm.amethyst.home.data

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

internal class DesktopDownloadedProjectDeletion(
    hubDirectory: Path = DesktopProjectStorage.directory.resolve("Hub"),
) {
    private val hubDirectory = hubDirectory.toAbsolutePath().normalize()

    fun targetFor(path: String): Path? {
        val file = Path.of(path).toAbsolutePath().normalize()
        val original = file.parent ?: return null
        val project = original.parent ?: return null

        if (original.fileName.toString() != "Original" || project.parent != hubDirectory) {
            return null
        }

        if (listOf(hubDirectory, project, original).any { Files.isSymbolicLink(it) }) {
            return null
        }

        return project
    }

    fun delete(path: String): Boolean = runCatching {
        val target = targetFor(path = path) ?: return false

        if (Files.notExists(target, NOFOLLOW_LINKS)) {
            return true
        }

        Files.walkFileTree(
            target,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    if (exc != null) {
                        throw exc
                    }

                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }
            },
        )

        true
    }.getOrDefault(defaultValue = false)
}
