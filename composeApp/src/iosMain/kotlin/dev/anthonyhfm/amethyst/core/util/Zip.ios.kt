package dev.anthonyhfm.amethyst.core.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy
import platform.zlib.*

@OptIn(ExperimentalForeignApi::class)
actual object Zip {
    // Desktop-ähnliche Funktionalität: wir iterieren lokale Datei-Header nacheinander.
    // Unterstützt Methoden: 0 (Stored) und 8 (Deflate). Andere werden übersprungen.

    actual fun getEntries(file: PlatformFile): List<ZipEntry> {
        val path = file.path
        if (path.isBlank()) return emptyList()
        val bytes = readAllBytes(path) ?: return emptyList()
        if (bytes.size < 30) return emptyList()

        val entries = mutableListOf<ZipEntry>()
        var offset = 0
        while (offset + 30 <= bytes.size) {
            val sig = readUInt32(bytes, offset)
            if (sig != LOCAL_FILE_HEADER_SIG) break // Ende oder beschädigt

            val flags = readUInt16(bytes, offset + 6)
            val method = readUInt16(bytes, offset + 8)
            val compSizeField = readUInt32(bytes, offset + 18)
            val uncompSizeField = readUInt32(bytes, offset + 22)
            val nameLen = readUInt16(bytes, offset + 26)
            val extraLen = readUInt16(bytes, offset + 28)

            val nameStart = offset + 30
            val nameEnd = nameStart + nameLen
            if (nameEnd > bytes.size) break
            val name = bytes.decodeToString(nameStart, nameEnd)

            val dataStart = nameEnd + extraLen
            if (dataStart > bytes.size) break

            var compSize = compSizeField
            var uncompSize = uncompSizeField

            // Flag Bit 3 (0x08) Data Descriptor vorhanden -> Komprimierte Größen erst nach Daten
            val hasDataDescriptor = (flags and 0x08) != 0
            if (hasDataDescriptor) {
                // Falls Größe unbekannt, versuchen wir heuristisch bis nächsten Header zu gehen.
                // Wir suchen Signatur des nächsten lokalen Headers oder des zentralen Headers.
                val nextHeaderIndex = findNextLocalOrCentralHeader(bytes, dataStart)
                if (nextHeaderIndex == -1) break
                compSize = nextHeaderIndex - dataStart
                // Unkomprimierte Größe können wir erst nach Dekompression kennen.
                uncompSize = 0
            }

            if (dataStart + compSize > bytes.size) break
            val compData = bytes.copyOfRange(dataStart, dataStart + compSize)

            val isDir = name.endsWith('/')
            val data: ByteArray = if (isDir) {
                ByteArray(0)
            } else {
                when (method) {
                    0 -> compData // STORED
                    8 -> inflateDeflate(compData, expectedSize = if (!hasDataDescriptor && uncompSize > 0) uncompSize else null)
                        ?: ByteArray(0)
                    else -> ByteArray(0) // Nicht unterstützt
                }
            }

            entries += ZipEntry(path = name, data = data, isDirectory = isDir)

            // Offset auf nächsten Header setzen
            offset = dataStart + compSize + if (hasDataDescriptor) skipDataDescriptor(bytes, dataStart + compSize) else 0
        }

        return entries
    }

    actual fun decode(file: String): ByteArray {
        // Desktop verwendet GZIPInputStream. Hier einfache Erkennung + Dekompression für GZIP (Deflate mit Header).
        if (file.isBlank()) return ByteArray(0)
        val bytes = readAllBytes(file) ?: return ByteArray(0)
        val isGzip = bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()
        return if (isGzip) gunzip(bytes) ?: ByteArray(0) else bytes
    }

    // -------------------- Hilfsfunktionen --------------------

    private const val LOCAL_FILE_HEADER_SIG = 0x04034b50
    private const val CENTRAL_FILE_HEADER_SIG = 0x02014b50

    private fun readAllBytes(path: String): ByteArray? {
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        val len = data.length.toInt()
        val out = ByteArray(len)
        out.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, len.convert()) }
        return out
    }

    private fun readUInt16(data: ByteArray, offset: Int): Int =
        if (offset + 1 < data.size) (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8) else 0

    private fun readUInt32(data: ByteArray, offset: Int): Int =
        if (offset + 3 < data.size)
            (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
        else 0

    private fun findNextLocalOrCentralHeader(bytes: ByteArray, start: Int): Int {
        var i = start
        val max = bytes.size - 4
        while (i <= max) {
            val sig = readUInt32(bytes, i)
            if (sig == LOCAL_FILE_HEADER_SIG || sig == CENTRAL_FILE_HEADER_SIG) return i
            i++
        }
        return -1
    }

    private fun skipDataDescriptor(bytes: ByteArray, descriptorStart: Int): Int {
        // Data Descriptor kann optional Signatur 0x08074b50 haben: [sig?][crc32][compSize][uncompSize]
        // Wir versuchen beide Varianten (mit und ohne Signatur) zu erkennen.
        val remaining = bytes.size - descriptorStart
        if (remaining < 12) return 0
        val sig = readUInt32(bytes, descriptorStart)
        return if (sig == 0x08074b50) {
            if (remaining >= 16) 16 else 0
        } else 12 // keine Signatur -> nur CRC + Größen
    }

    private fun gunzip(data: ByteArray): ByteArray? = memScoped {
        val strm = alloc<z_stream>()
        strm.zalloc = null; strm.zfree = null; strm.opaque = null
        val rc = inflateInit2_(strm.ptr, 16 + 15, ZLIB_VERSION, sizeOf<z_stream>().convert())
        if (rc != Z_OK) return null
        try {
            var out = ByteArray((data.size * 3).coerceAtLeast(64 * 1024))
            var written = 0
            data.usePinned { inPinned ->
                strm.next_in = inPinned.addressOf(0).reinterpret()
                strm.avail_in = data.size.convert()
                while (true) {
                    if (written == out.size) out = out.copyOf(out.size * 2)
                    out.usePinned { outPinned ->
                        strm.next_out = (outPinned.addressOf(0) + written)?.reinterpret()
                        strm.avail_out = (out.size - written).convert()
                        val r = inflate(strm.ptr, Z_NO_FLUSH)
                        val produced = (out.size - written) - strm.avail_out.toInt()
                        written += produced
                        when (r) {
                            Z_STREAM_END -> break
                            Z_OK -> if (strm.avail_in.toInt() == 0 && produced == 0) break
                            Z_BUF_ERROR -> if (strm.avail_in.toInt() == 0) break else return null
                            else -> return null
                        }
                    }
                }
            }
            out.copyOf(written)
        } finally { inflateEnd(strm.ptr) }
    }

    private fun inflateDeflate(data: ByteArray, expectedSize: Int? = null): ByteArray? = memScoped {
        if (data.isEmpty()) return ByteArray(0)
        val strm = alloc<z_stream>()
        strm.zalloc = null; strm.zfree = null; strm.opaque = null
        val rc = inflateInit2_(strm.ptr, -15, ZLIB_VERSION, sizeOf<z_stream>().convert())
        if (rc != Z_OK) return null
        try {
            var out = ByteArray(expectedSize ?: (data.size * 3).coerceAtLeast(256))
            var written = 0
            data.usePinned { inPinned ->
                strm.next_in = inPinned.addressOf(0).reinterpret()
                strm.avail_in = data.size.convert()
                var guard = 0
                while (true) {
                    guard++
                    if (guard > 10000) return null // Sicherheit
                    if (written == out.size) out = out.copyOf(out.size * 2)
                    out.usePinned { outPinned ->
                        strm.next_out = (outPinned.addressOf(0) + written)?.reinterpret()
                        strm.avail_out = (out.size - written).convert()
                        val r = inflate(strm.ptr, Z_NO_FLUSH)
                        val produced = (out.size - written) - strm.avail_out.toInt()
                        written += produced
                        when (r) {
                            Z_STREAM_END -> break
                            Z_OK -> if (strm.avail_in.toInt() == 0 && produced == 0) break
                            Z_BUF_ERROR -> if (strm.avail_in.toInt() == 0) break else return null
                            else -> return null
                        }
                    }
                    if (strm.avail_in.toInt() == 0) break
                }
            }
            out.copyOf(written)
        } finally { inflateEnd(strm.ptr) }
    }
}