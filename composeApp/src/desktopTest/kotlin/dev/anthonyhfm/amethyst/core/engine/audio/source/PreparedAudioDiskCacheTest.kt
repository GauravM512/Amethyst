package dev.anthonyhfm.amethyst.core.engine.audio.source

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PreparedAudioDiskCacheTest {
    @Test
    fun preparedPcmSurvivesMemoryCacheClearAndInvalidatesWhenSourceChanges() {
        val directory = Files.createTempDirectory("amethyst-prepared-test").toFile()
        try {
            PreparedAudioSourceCache.configurePersistentRoot(directory.path)
            val source = ByteArrayPcmAudioSource(
                id = "source-1",
                sampleRate = 22_050,
                channels = 1,
                bitDepth = 16,
                rawData = byteArrayOf(0, 0, 0, 32, 0, 64, 0, 0),
            )
            val first = PreparedAudioSourceCache.getOrPrepare(source, 44_100) as ByteArrayPcmAudioSource
            assertEquals(1, directory.listFiles()?.size)

            PreparedAudioSourceCache.clear()
            val reopened = PreparedAudioSourceCache.getOrPrepare(source, 44_100) as ByteArrayPcmAudioSource
            assertContentEquals(first.rawData, reopened.rawData)

            val edited = ByteArrayPcmAudioSource(
                id = source.id,
                sampleRate = source.sampleRate,
                channels = source.channels,
                bitDepth = source.bitDepth,
                rawData = byteArrayOf(0, 0, 0, 64, 0, 32, 0, 0),
            )
            val updated = PreparedAudioSourceCache.getOrPrepare(edited, 44_100) as ByteArrayPcmAudioSource
            assertFalse(first.rawData.contentEquals(updated.rawData))
            assertEquals(2, directory.listFiles()?.size)
        } finally {
            PreparedAudioSourceCache.configurePersistentRoot(null)
            PreparedAudioSourceCache.clear()
            directory.deleteRecursively()
        }
    }
}
