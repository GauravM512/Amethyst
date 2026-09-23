package dev.anthonyhfm.amethyst.conversion.ableton

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.ferror
import platform.posix.fopen
import platform.posix.fread
import kotlinx.cinterop.CPointer

/** xmlutil's native InputStreamReader can address one past its full 8 KB input buffer. */
@OptIn(ExperimentalForeignApi::class, XmlUtilInternal::class)
internal class IosUtf8FileReader(path: String) : Reader() {
    private val file: CPointer<FILE> = fopen(path, "rb") ?: error("Could not open Ableton XML")
    private val bytes = ByteArray(64 * 1024)
    private var position = 0
    private var end = 0
    private var pendingLowSurrogate: Char? = null
    private var closed = false

    override fun read(buf: CharArray, offset: Int, len: Int): Int {
        if (len == 0) return 0
        var written = 0
        pendingLowSurrogate?.let {
            buf[offset + written++] = it
            pendingLowSurrogate = null
        }
        while (written < len) {
            val lead = nextByte()
            if (lead < 0) break
            val codePoint = when {
                lead < 0x80 -> lead
                lead in 0xC2..0xDF -> ((lead and 0x1F) shl 6) or continuation()
                lead in 0xE0..0xEF -> ((lead and 0x0F) shl 12) or
                    (continuation() shl 6) or continuation()
                lead in 0xF0..0xF4 -> ((lead and 0x07) shl 18) or
                    (continuation() shl 12) or (continuation() shl 6) or continuation()
                else -> error("Invalid UTF-8 in Ableton XML")
            }
            if (codePoint <= 0xFFFF) {
                buf[offset + written++] = codePoint.toChar()
            } else {
                val value = codePoint - 0x10000
                buf[offset + written++] = (0xD800 + (value shr 10)).toChar()
                val low = (0xDC00 + (value and 0x3FF)).toChar()
                if (written < len) buf[offset + written++] = low
                else pendingLowSurrogate = low
            }
        }
        return if (written == 0) -1 else written
    }

    override fun close() {
        if (!closed) {
            closed = true
            fclose(file)
        }
    }

    private fun continuation(): Int {
        val next = nextByte()
        if (next !in 0x80..0xBF) error("Invalid UTF-8 continuation in Ableton XML")
        return next and 0x3F
    }

    private fun nextByte(): Int {
        if (position == end) {
            val count = bytes.usePinned { pinned ->
                fread(pinned.addressOf(0), 1uL, bytes.size.toULong(), file).toInt()
            }
            if (count == 0) {
                if (ferror(file) != 0) error("Could not read Ableton XML")
                return -1
            }
            position = 0
            end = count
        }
        return bytes[position++].toInt() and 0xFF
    }
}
