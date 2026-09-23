package dev.anthonyhfm.amethyst.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProjectArchiveFormatTest {
    @Test
    fun recognizesNestedAbletonAndUniPadArchives() {
        assertEquals(
            ZippedProjectFormat.ABLETON,
            determineProjectArchiveFormat(listOf("Project/Live Set.als", "Project/Samples/kick.wav")),
        )
        assertEquals(
            ZippedProjectFormat.ABLETON_APOLLO,
            determineProjectArchiveFormat(listOf("Project/Live Set.als", "Project/Lights.approj")),
        )
        assertEquals(
            ZippedProjectFormat.UNIPAD,
            determineProjectArchiveFormat(listOf("UniPack/info", "UniPack/keySound")),
        )
    }

    @Test
    fun missingOrUnrelatedEntriesAreNotUniPad() {
        assertFailsWith<IllegalArgumentException> { determineProjectArchiveFormat(emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            determineProjectArchiveFormat(listOf("samples/kick.wav", "readme.txt"))
        }
    }
}
