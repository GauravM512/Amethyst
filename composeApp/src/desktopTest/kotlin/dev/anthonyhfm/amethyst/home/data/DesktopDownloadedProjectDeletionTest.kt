package dev.anthonyhfm.amethyst.home.data

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopDownloadedProjectDeletionTest {
    @Test
    fun deletesTheEntireDownloadAndPreparedFilesWhileKeepingOtherProjects() {
        withTemporaryDirectory { root ->
            val hub = root.resolve("Hub")
            val original = writeFile(path = hub.resolve("download/Original/project.ame"))
            val prepared = writeFile(path = hub.resolve("download/Prepared/hash/workspace.ame"))
            val sibling = writeFile(path = hub.resolve("other/Original/project.ame"))
            val deletion = DesktopDownloadedProjectDeletion(hubDirectory = hub)

            assertTrue(actual = deletion.delete(path = original.toString()))
            assertFalse(actual = Files.exists(original))
            assertFalse(actual = Files.exists(prepared))
            assertFalse(actual = Files.exists(hub.resolve("download")))
            assertTrue(actual = Files.exists(sibling))
        }
    }

    @Test
    fun refusesFilesOutsideADownloadsOriginalDirectory() {
        withTemporaryDirectory { root ->
            val hub = root.resolve("Hub")
            val deletion = DesktopDownloadedProjectDeletion(hubDirectory = hub)
            val unrelated = writeFile(path = root.resolve("Projects/local/Original/project.ame"))
            val misplaced = writeFile(path = hub.resolve("download/project.ame"))

            assertFalse(actual = deletion.delete(path = unrelated.toString()))
            assertFalse(actual = deletion.delete(path = misplaced.toString()))
            assertFalse(actual = deletion.delete(path = hub.toString()))
            assertTrue(actual = Files.exists(unrelated))
            assertTrue(actual = Files.exists(misplaced))
        }
    }

    @Test
    fun deletesContainedSymbolicLinksWithoutFollowingThem() {
        withTemporaryDirectory { root ->
            val hub = root.resolve("Hub")
            val original = writeFile(path = hub.resolve("download/Original/project.ame"))
            val external = writeFile(path = root.resolve("external/keep.ame"))
            Files.createSymbolicLink(hub.resolve("download/Prepared"), external.parent)

            assertTrue(actual = DesktopDownloadedProjectDeletion(hubDirectory = hub).delete(path = original.toString()))
            assertTrue(actual = Files.exists(external))
        }
    }

    @Test
    fun refusesSymbolicLinksInTheDownloadDirectoryPath() {
        withTemporaryDirectory { root ->
            val hub = Files.createDirectories(root.resolve("Hub"))
            val external = writeFile(path = root.resolve("external/Original/project.ame"))
            val linkedProject = hub.resolve("linked")
            Files.createSymbolicLink(linkedProject, external.parent.parent)
            val deletion = DesktopDownloadedProjectDeletion(hubDirectory = hub)

            assertNull(actual = deletion.targetFor(path = linkedProject.resolve("Original/project.ame").toString()))
            assertFalse(actual = deletion.delete(path = linkedProject.resolve("Original/project.ame").toString()))

            val otherProject = Files.createDirectories(hub.resolve("other"))
            Files.createSymbolicLink(otherProject.resolve("Original"), external.parent)
            assertFalse(actual = deletion.delete(path = otherProject.resolve("Original/project.ame").toString()))

            val linkedHub = root.resolve("LinkedHub")
            Files.createSymbolicLink(linkedHub, hub)
            assertFalse(
                actual = DesktopDownloadedProjectDeletion(hubDirectory = linkedHub)
                    .delete(path = linkedHub.resolve("linked/Original/project.ame").toString()),
            )
            assertTrue(actual = Files.exists(external))
        }
    }

    @Test
    fun allowsRemovalOfAMissingDownloadFromTheCatalog() {
        withTemporaryDirectory { root ->
            val hub = root.resolve("Hub")
            val deletion = DesktopDownloadedProjectDeletion(hubDirectory = hub)

            assertTrue(actual = deletion.delete(path = hub.resolve("missing/Original/project.ame").toString()))
        }
    }

    private fun writeFile(path: Path): Path {
        Files.createDirectories(path.parent)
        return Files.writeString(path, "project")
    }

    private fun withTemporaryDirectory(block: (Path) -> Unit) {
        val root = Files.createTempDirectory("amethyst-project-deletion-")

        try {
            block(root)
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
