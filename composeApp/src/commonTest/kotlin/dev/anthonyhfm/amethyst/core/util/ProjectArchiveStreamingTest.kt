package dev.anthonyhfm.amethyst.core.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.runBlocking
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class ProjectArchiveStreamingTest {
    @Test
    fun deflatedEntryCanBeExtractedWithoutWholeArchiveMaterialization() = runBlocking {
        val archivePath = ConversionTempFiles.newPath("zip")
        val extractedPath = ConversionTempFiles.newPath("wav")
        try {
            PlatformFile(archivePath).write(Base64.decode(ZIP_FIXTURE))
            val reader = requireNotNull(Zip.open(PlatformFile(archivePath)))
            try {
                assertTrue(reader.extractEntryToFile("samples/test.wav", extractedPath))
                val expected = ByteArray(1024) { (it % 256).toByte() }
                assertContentEquals(expected, PlatformFile(extractedPath).readBytes())
                assertContentEquals(expected, reader.readEntry("samples/test.wav"))
            } finally {
                reader.close()
            }
        } finally {
            ConversionTempFiles.remove(extractedPath)
            ConversionTempFiles.remove(archivePath)
        }
    }

    private companion object {
        const val ZIP_FIXTURE = "UEsDBBQAAAAIACpKN10mTAu3GAEAAAAEAAAQAAAAc2FtcGxlcy90ZXN0LndhdmNgZGJmYWVj5+Dk4ubh5eMXEBQSFhEVE5eQlJKWkZWTV1BUUlZRVVPX0NTS1tHV0zcwNDI2MTUzt7C0sraxtbN3cHRydnF1c/fw9PL28fXzDwgMCg4JDQuPiIyKjomNi09ITEpOSU1Lz8jMys7JzcsvKCwqLiktK6+orKquqa2rb2hsam5pbWvv6Ozq7unt658wcdLkKVOnTZ8xc9bsOXPnzV+wcNHiJUuXLV+xctXqNWvXrd+wcdPmLVu3bd+xc9fuPXv37T9w8NDhI0ePHT9x8tTpM2fPnb9w8dLlK1evXb9x89btO3fv3X/w8NHjJ0+fPX/x8tXrN2/fvf/w8dPnL1+/ff/x89fvP3///WcY9f+o/0ew/wFQSwECFAMUAAAACAAqSjddJkwLtxgBAAAABAAAEAAAAAAAAAAAAAAAgAEAAAAAc2FtcGxlcy90ZXN0LndhdlBLBQYAAAAAAQABAD4AAABGAQAAAAA="
    }
}
