package dev.anthonyhfm.amethyst.conversion.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.data.Ableton
import nl.adaptivity.xmlutil.xmlStreaming
import nl.adaptivity.xmlutil.serialization.XML
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

actual object AbletonXmlDecoder {
    actual fun decodeFile(path: String, xml: XML): Ableton =
        FileInputStream(path).use { source ->
            GZIPInputStream(source, 64 * 1024).use { gzip ->
                xmlStreaming.newReader(AbletonSanitizingReader(InputStreamReader(gzip, Charsets.UTF_8))).use { reader ->
                    xml.decodeFromReader(Ableton.serializer(), reader)
                }
            }
        }
}
