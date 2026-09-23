package dev.anthonyhfm.amethyst.home.data

import dev.anthonyhfm.amethyst.timeline.data.AudioSource
import dev.anthonyhfm.amethyst.workspace.data.SavableWorkspaceData
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class MobileProjectBundleCodecTest {
    @Test
    fun headerKeepsAudioMetadataWithoutEmbeddingPcm() {
        val pcm = ByteArray(12_000) { it.toByte() }
        val source = AudioSource(
            id = "sample-1",
            fileName = "kick.wav",
            rawData = pcm,
            sampleRate = 48_000,
            channels = 2,
            bitDepth = 24,
        )
        val original = SavableWorkspaceData(title = "Test project", audioSources = listOf(source))

        val header = MobileProjectBundleCodec.decodeHeader(MobileProjectBundleCodec.encodeHeader(original))

        assertEquals("Test project", header.title)
        assertEquals("sample-1", header.audioSources.single().id)
        assertEquals(48_000, header.audioSources.single().sampleRate)
        assertContentEquals(ByteArray(0), header.audioSources.single().rawData)
        assertContentEquals(pcm, original.audioSources.single().rawData)
    }
}
