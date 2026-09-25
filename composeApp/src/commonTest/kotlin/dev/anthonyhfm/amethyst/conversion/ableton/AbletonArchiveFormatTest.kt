package dev.anthonyhfm.amethyst.conversion.ableton

import dev.anthonyhfm.amethyst.core.util.ZippedProjectFormat
import dev.anthonyhfm.amethyst.core.util.determineProjectArchiveFormat
import kotlin.test.Test
import kotlin.test.assertEquals

class AbletonArchiveFormatTest {
    @Test
    fun recognizesNestedLiveSetsRegardlessOfExtensionCase() {
        assertEquals(
            ZippedProjectFormat.ABLETON,
            determineProjectArchiveFormat(listOf("Project/Live Set.ALS", "Project/Samples/kick.wav")),
        )
    }

    @Test
    fun recognizesCombinedAbletonApolloArchives() {
        assertEquals(
            ZippedProjectFormat.ABLETON_APOLLO,
            determineProjectArchiveFormat(listOf("Song.als", "Lights.APPROJ")),
        )
    }

    @Test
    fun unrelatedArchivesKeepUnipadFormat() {
        assertEquals(
            ZippedProjectFormat.UNIPAD,
            determineProjectArchiveFormat(listOf("keySound", "info", "sounds/36.wav")),
        )
    }
}
