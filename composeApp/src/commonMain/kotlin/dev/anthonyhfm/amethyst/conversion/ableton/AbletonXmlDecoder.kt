package dev.anthonyhfm.amethyst.conversion.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.data.Ableton
import nl.adaptivity.xmlutil.serialization.XML

/** Decode a gzip .als directly from disk without constructing a full XML String. */
expect object AbletonXmlDecoder {
    fun decodeFile(path: String, xml: XML): Ableton
}
