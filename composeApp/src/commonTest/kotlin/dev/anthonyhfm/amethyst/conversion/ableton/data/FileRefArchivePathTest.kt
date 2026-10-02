package dev.anthonyhfm.amethyst.conversion.ableton.data

import kotlin.test.Test
import kotlin.test.assertEquals

class FileRefArchivePathTest {
    @Test
    fun absoluteReferenceInsideArchivedProjectResolvesWithoutNameOrRelativePath() {
        val reference = FileRef(
            relativePath = FileRef.RelativePath(),
            path = FileRef.Path(
                value = "/Users/creator/Uploads/Archived Project/Samples/Processed/Crop/Drums.wav"
            ),
            type = FileRef.Type(value = 1)
        )

        val archivePath = "Archived Project/Samples/Processed/Crop/Drums.wav"

        assertEquals(
            expected = archivePath,
            actual = reference.resolvePath(
                projectPath = "Archived Project",
                isZip = true,
                zipEntryExists = { it == archivePath }
            )
        )
    }

    @Test
    fun unrelatedAbsoluteReferenceDoesNotMatchAnArchiveFileByBasename() {
        val absolutePath = "/Users/creator/Other Project/Samples/Drums.wav"
        val reference = FileRef(
            relativePath = FileRef.RelativePath(),
            path = FileRef.Path(value = absolutePath),
            type = FileRef.Type(value = 1)
        )

        assertEquals(
            expected = absolutePath,
            actual = reference.resolvePath(
                projectPath = "Archived Project",
                isZip = true,
                zipEntryExists = { it == "Archived Project/Samples/Drums.wav" }
            )
        )
    }
}
