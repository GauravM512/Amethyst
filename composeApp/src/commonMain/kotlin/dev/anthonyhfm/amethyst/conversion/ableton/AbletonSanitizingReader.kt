package dev.anthonyhfm.amethyst.conversion.ableton

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader

/** Keeps Ableton's whitespace normalization without materializing the entire XML document. */
@OptIn(XmlUtilInternal::class)
internal class AbletonSanitizingReader(private val source: Reader) : Reader() {
    private val buffer = CharArray(64 * 1024)
    private var position = 0
    private var end = 0
    private var previousSourceChar = '\u0000'

    override fun read(buf: CharArray, offset: Int, len: Int): Int {
        if (len == 0) return 0
        var written = 0
        while (written < len) {
            val char = nextChar()
            if (char < 0) break
            val value = char.toChar()
            val skip = value == '\n' || value == '\r' || value == '\t' ||
                (value == ' ' && (previousSourceChar == '>' || peekChar() == '<'.code))
            previousSourceChar = value
            if (!skip) buf[offset + written++] = value
        }
        return if (written == 0) -1 else written
    }

    override fun close() = source.close()

    private fun nextChar(): Int {
        if (!refill()) return -1
        return buffer[position++].code
    }

    private fun peekChar(): Int {
        if (!refill()) return -1
        return buffer[position].code
    }

    private fun refill(): Boolean {
        if (position < end) return true
        val count = source.read(buffer, 0, buffer.size)
        if (count <= 0) return false
        position = 0
        end = count
        return true
    }
}
