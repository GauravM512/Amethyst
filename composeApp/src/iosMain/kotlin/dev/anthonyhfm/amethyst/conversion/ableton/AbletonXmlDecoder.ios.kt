package dev.anthonyhfm.amethyst.conversion.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.data.Ableton
import dev.anthonyhfm.amethyst.core.util.ConversionTempFiles
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import nl.adaptivity.xmlutil.xmlStreaming
import nl.adaptivity.xmlutil.serialization.XML
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.zlib.gzclose
import platform.zlib.gzopen
import platform.zlib.gzread

@OptIn(ExperimentalForeignApi::class)
actual object AbletonXmlDecoder {
    actual fun decodeFile(path: String, xml: XML): Ableton {
        val temporaryXml = ConversionTempFiles.newPath("xml")
        try {
            val compressed = gzopen(path, "rb") ?: error("Could not open Ableton set: $path")
            try {
                val output = fopen(temporaryXml, "wb") ?: error("Could not create temporary Ableton XML")
                try {
                    val buffer = ByteArray(64 * 1024)
                    buffer.usePinned { pinned ->
                        while (true) {
                            val count = gzread(compressed, pinned.addressOf(0), buffer.size.toUInt())
                            if (count < 0) error("Invalid gzip data in Ableton set")
                            if (count == 0) break
                            check(fwrite(pinned.addressOf(0), 1uL, count.toULong(), output).toInt() == count) {
                                "Could not write temporary Ableton XML"
                            }
                        }
                    }
                } finally {
                    fclose(output)
                }
            } finally {
                gzclose(compressed)
            }
            val textReader = IosUtf8FileReader(temporaryXml)
            val reader = xmlStreaming.newReader(AbletonSanitizingReader(textReader))
            return try {
                xml.decodeFromReader(Ableton.serializer(), reader)
            } finally {
                reader.close()
                textReader.close()
            }
        } finally {
            ConversionTempFiles.remove(temporaryXml)
        }
    }
}
