package dev.anthonyhfm.amethyst.conversion.ableton.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FileRefTest {
    private fun ref(
        name: String? = null,
        dirs: List<String> = emptyList(),
        relative: String? = null,
        absolute: String? = null,
    ) = FileRef(
        relativePath = FileRef.RelativePath(
            value = relative,
            items = dirs.map { FileRef.RelativePath.RelativePathElement(it) },
        ),
        path = absolute?.let(FileRef::Path),
        name = name?.let(FileRef::Name),
        type = FileRef.Type(1),
    )

    @Test
    fun searchesNestedSamplePathsFromMostSpecificToLeastSpecific() {
        assertEquals(
            listOf("Project/Samples/Drums/Kick.wav", "Project/Drums/Kick.wav", "Project/Kick.wav"),
            ref("Kick.wav", listOf("Samples", "Drums")).resolvePathCandidates("Project"),
        )
    }

    @Test
    fun zipResolutionFindsExistingFallbackEntry() {
        val reference = ref("Kick.wav", listOf("Missing", "Drums"))
        assertEquals(
            "Project/Drums/Kick.wav",
            reference.resolvePath("Project", isZip = true, zipEntryExists = { it == "Project/Drums/Kick.wav" }),
        )
    }

    @Test
    fun looseFileResolutionUsesExistingAbsolutePath() {
        val reference = ref(relative = "missing.wav", absolute = "/audio/Kick.wav")
        assertEquals(
            "/audio/Kick.wav",
            reference.resolvePath("Project", isZip = false, fileExists = { it == "/audio/Kick.wav" }),
        )
    }

    @Test
    fun unresolvedReferencesKeepFirstCandidateAndEmptyReferencesFail() {
        assertEquals("Project/Samples/Kick.wav", ref("Kick.wav", listOf("Samples"))
            .resolvePath("Project", isZip = true))
        assertFailsWith<IllegalStateException> { ref().resolvePath("Project", isZip = false) }
    }

    @Test
    fun coreLibraryAbsoluteReferenceSearchesOtherLiveVersions() {
        val candidates = ref(absolute = "/Applications/Ableton Live 11 Suite.app/Contents/Samples/Kick.wav")
            .resolvePathCandidates("Project")
        assertTrue(candidates.contains("/Applications/Ableton Live 12 Suite.app/Contents/Samples/Kick.wav"))
    }
}
