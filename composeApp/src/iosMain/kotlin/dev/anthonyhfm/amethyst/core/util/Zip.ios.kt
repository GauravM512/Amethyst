package dev.anthonyhfm.amethyst.core.util

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy
import platform.zlib.*

@OptIn(ExperimentalForeignApi::class)
actual object Zip {
    private const val LOCAL_FILE_HEADER_SIG = 0x04034b50
    private const val CENTRAL_FILE_HEADER_SIG = 0x02014b50
    private const val EOCD_SIG = 0x06054b50
    private const val GZIP_WINDOW_BITS = 16 + 15
    private const val RAW_DEFLATE_WINDOW_BITS = -15
    private const val STREAMING_THRESHOLD = 20 * 1024 * 1024 // 20MB

    actual fun getEntries(file: String): List<ZipEntry> {
        val data = readFileBytes(file) ?: return emptyList()
        val entries = parseCentralDirectory(data)
        return entries.map { ZipEntry(it.path, it.isDirectory) }
    }

    actual fun getInputStream(zipPath: String, file: String): ByteArray {
        val data = readFileBytes(zipPath) ?: return ByteArray(0)
        val entries = parseCentralDirectory(data)
        val target = entries.firstOrNull { it.path == file } ?: return ByteArray(0)
        return extractEntryData(data, target) ?: ByteArray(0)
    }

    actual fun decode(file: String): ByteArray {
        val size = fileSize(file) ?: -1L
        println("[Zip.iOS] decode: path=$file reportedSize=$size")
        if (size == 0L) return ByteArray(0)

        // Large file -> stream directly
        if (size > STREAMING_THRESHOLD) {
            val streamed = gunzipFileStreaming(file)
            if (streamed != null && streamed.isNotEmpty()) {
                println("[Zip.iOS] decode: streaming decompressed size=${streamed.size}")
                return streamed
            }
            println("[Zip.iOS] decode: streaming fallback failed, trying in-memory")
        }

        val compressed = readFileBytes(file)
        if (compressed == null) {
            println("[Zip.iOS] decode: failed to read file (null) $file")
            return ByteArray(0)
        }
        if (compressed.isEmpty()) {
            println("[Zip.iOS] decode: in-memory read empty while size=$size -> try streaming fallback")
            val streamed = gunzipFileStreaming(file)
            if (streamed != null && streamed.isNotEmpty()) return streamed
            return ByteArray(0)
        }
        val isGzip = compressed.size >= 2 && compressed[0] == 0x1F.toByte() && compressed[1] == 0x8B.toByte()
        val isZip = compressed.size >= 4 && compressed[0] == 'P'.code.toByte() && compressed[1] == 'K'.code.toByte() && compressed[2] == 0x03.toByte() && compressed[3] == 0x04.toByte()
        println("[Zip.iOS] decode: bufferSize=${compressed.size} gzipMagic=$isGzip zipMagic=$isZip")
        if (isZip) {
            // Treat as ZIP and extract first candidate file
            val entries = parseCentralDirectory(compressed)
            println("[Zip.iOS] decode: zip fallback entries=${entries.size}")
            val candidate = entries.firstOrNull { !it.isDirectory && (it.path.endsWith(".als") || it.path.endsWith(".xml")) }
                ?: entries.firstOrNull { !it.isDirectory }
            if (candidate != null) {
                val data = extractEntryData(compressed, candidate)
                if (data != null && data.isNotEmpty()) {
                    val innerIsGzip = data.size >= 2 && data[0] == 0x1F.toByte() && data[1] == 0x8B.toByte()
                    println("[Zip.iOS] decode: inner entry ${candidate.path} size=${data.size} innerGzip=$innerIsGzip")
                    return if (innerIsGzip) gunzipDirect(data) ?: data else data
                }
            }
        }
        if (!isGzip && compressed[0].toInt().toChar() == '<') return compressed
        val decompressed = if (isGzip) gunzipDirect(compressed) else null
        if (decompressed == null || decompressed.isEmpty()) {
            println("[Zip.iOS] decode: gunzipDirect empty -> streaming retry")
            val streamed = if (isGzip) gunzipFileStreaming(file) else null
            if (streamed != null && streamed.isNotEmpty()) return streamed
            return if (compressed[0].toInt().toChar() == '<') compressed else ByteArray(0)
        }
        return decompressed
    }

    private data class InternalEntry(
        val path: String,
        val isDirectory: Boolean,
        val compressionMethod: Int,
        val compressedSize: Int,
        val uncompressedSize: Int,
        val dataOffset: Int,
        val flags: Int
    )

    private fun fileSize(path: String): Long? = autoreleasepool {
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null) ?: return@autoreleasepool null
        (attrs[NSFileSize] as? NSNumber)?.longLongValue
    }

    private fun withSecurityScoped(path: String, block: () -> ByteArray?): ByteArray? {
        val url = NSURL.fileURLWithPath(path)
        val needs = url?.isFileURL == true && url.startAccessingSecurityScopedResource()
        return try { block() } finally { if (needs) url.stopAccessingSecurityScopedResource() }
    }

    private fun readFileBytes(path: String): ByteArray? = withSecurityScoped(path) {
        val nsData = NSData.dataWithContentsOfFile(path)
        if (nsData == null) {
            println("[Zip.iOS] readFileBytes: NSData null for $path")
            return@withSecurityScoped null
        }
        val length = nsData.length.toInt()
        if (length == 0) {
            println("[Zip.iOS] readFileBytes: NSData length 0 for $path")
            return@withSecurityScoped ByteArray(0)
        }
        return@withSecurityScoped ByteArray(length).apply {
            usePinned { pinned -> memcpy(pinned.addressOf(0), nsData.bytes, length.convert()) }
        }
    }

    // Streaming gzip decompression directly from file (avoids loading full compressed content)
    private fun gunzipFileStreaming(path: String): ByteArray? = memScoped {
        val handle = NSFileHandle.fileHandleForReadingAtPath(path) ?: run {
            println("[Zip.iOS] gunzipFileStreaming: cannot open $path")
            return null
        }
        try {
            val stream = alloc<z_stream>()
            stream.zalloc = null; stream.zfree = null; stream.opaque = null
            val initRc = inflateInit2_(stream.ptr, GZIP_WINDOW_BITS, ZLIB_VERSION, sizeOf<z_stream>().convert())
            if (initRc != Z_OK) {
                println("[Zip.iOS] gunzipFileStreaming: inflateInit2 failed rc=$initRc")
                return null
            }
            val outParts = ArrayList<ByteArray>(16)
            var total = 0
            try {
                var finished = false
                while (!finished) {
                    val chunkData = handle.readDataOfLength((64 * 1024).convert())
                    val chunkLen = chunkData.length.toInt()
                    if (chunkLen == 0) break
                    val inBuf = ByteArray(chunkLen)
                    inBuf.usePinned { pin -> memcpy(pin.addressOf(0), chunkData.bytes, chunkLen.convert()) }
                    var inOffset = 0
                    while (inOffset < inBuf.size) {
                        val remaining = inBuf.size - inOffset
                        inBuf.usePinned { pinIn ->
                            stream.next_in = (pinIn.addressOf(0) + inOffset).reinterpret()
                            stream.avail_in = remaining.convert()
                            // Consume this provided avail_in fully via loop
                            while (stream.avail_in.toInt() > 0) {
                                val outChunk = ByteArray(64 * 1024)
                                outChunk.usePinned { pinOut ->
                                    stream.next_out = pinOut.addressOf(0).reinterpret()
                                    stream.avail_out = outChunk.size.convert()
                                    val rc = inflate(stream.ptr, Z_NO_FLUSH)
                                    val produced = outChunk.size - stream.avail_out.toInt()
                                    if (produced > 0) {
                                        outParts += if (produced == outChunk.size) outChunk else outChunk.copyOf(produced)
                                        total += produced
                                    }
                                    when (rc) {
                                        Z_STREAM_END -> { finished = true; return@usePinned }
                                        Z_OK -> { /* continue */ }
                                        Z_BUF_ERROR -> { if (produced == 0) return@usePinned }
                                        else -> { println("[Zip.iOS] gunzipFileStreaming: inflate rc=$rc"); return null }
                                    }
                                }
                                if (finished) break
                                if (stream.avail_in.toInt() == 0) break
                            }
                            // Adjust inOffset by bytes consumed (original remaining - stream.avail_in)
                            val consumed = remaining - stream.avail_in.toInt()
                            inOffset += consumed
                        }
                        if (finished) break
                    }
                }
            } finally { inflateEnd(stream.ptr) }
            if (total == 0) return null
            val result = ByteArray(total)
            var pos = 0
            for (p in outParts) { p.copyInto(result, pos); pos += p.size }
            result
        } catch (t: Throwable) {
            println("[Zip.iOS] gunzipFileStreaming error: ${t.message}")
            null
        } finally {
            try { handle.closeFile() } catch (_: Throwable) {}
        }
    }

    // Central Directory parsing (supports data descriptor entries)
    private fun parseCentralDirectory(zip: ByteArray): List<InternalEntry> {
        val eocdOffset = findEOCD(zip) ?: return emptyList()
        if (eocdOffset + 22 > zip.size) return emptyList()
        val totalEntries = readUInt16(zip, eocdOffset + 10)
        val centralDirSize = readUInt32(zip, eocdOffset + 12)
        val centralDirOffset = readUInt32(zip, eocdOffset + 16)
        if (centralDirOffset + centralDirSize > zip.size) return emptyList()
        val list = ArrayList<InternalEntry>(totalEntries)
        var ptr = centralDirOffset
        repeat(totalEntries) {
            if (ptr + 46 > zip.size) return@repeat
            val sig = readUInt32(zip, ptr)
            if (sig != CENTRAL_FILE_HEADER_SIG) return@repeat
            val flags = readUInt16(zip, ptr + 8)
            val compression = readUInt16(zip, ptr + 10)
            val compressedSize = readUInt32(zip, ptr + 20)
            val uncompressedSize = readUInt32(zip, ptr + 24)
            val nameLen = readUInt16(zip, ptr + 28)
            val extraLen = readUInt16(zip, ptr + 30)
            val commentLen = readUInt16(zip, ptr + 32)
            val localHeaderRelOffset = readUInt32(zip, ptr + 42)
            val nameStart = ptr + 46
            if (nameStart + nameLen > zip.size) return@repeat
            val name = zip.decodeToString(nameStart, nameStart + nameLen)
            // Advance pointer to next central header
            ptr = nameStart + nameLen + extraLen + commentLen
            val isDir = name.endsWith("/")
            // Parse local header to compute data offset
            val dataOffset = computeDataOffset(zip, localHeaderRelOffset) ?: return@repeat
            val sizes = resolveSizes(zip, localHeaderRelOffset, flags, compressedSize, uncompressedSize)
            list += InternalEntry(
                path = name,
                isDirectory = isDir,
                compressionMethod = compression,
                compressedSize = sizes.first,
                uncompressedSize = sizes.second,
                dataOffset = dataOffset,
                flags = flags
            )
        }
        return list
    }

    private fun computeDataOffset(zip: ByteArray, localHeaderOffset: Int): Int? {
        if (localHeaderOffset + 30 > zip.size) return null
        val sig = readUInt32(zip, localHeaderOffset)
        if (sig != LOCAL_FILE_HEADER_SIG) return null
        val nameLen = readUInt16(zip, localHeaderOffset + 26)
        val extraLen = readUInt16(zip, localHeaderOffset + 28)
        val start = localHeaderOffset + 30 + nameLen + extraLen
        return if (start <= zip.size) start else null
    }

    // If bit 3 (0x08) is set, sizes are only valid in central directory
    private fun resolveSizes(zip: ByteArray, localHeaderOffset: Int, flags: Int, cdCompressed: Int, cdUncompressed: Int): Pair<Int, Int> {
        if ((flags and 0x08) != 0) return Pair(cdCompressed, cdUncompressed)
        if (localHeaderOffset + 30 > zip.size) return Pair(cdCompressed, cdUncompressed)
        val compressed = readUInt32(zip, localHeaderOffset + 18)
        val uncompressed = readUInt32(zip, localHeaderOffset + 22)
        return Pair(if (compressed != 0) compressed else cdCompressed, if (uncompressed != 0) uncompressed else cdUncompressed)
    }

    private fun findEOCD(zip: ByteArray): Int? {
        // EOCD record is at end, variable comment length (0-65535). Search backwards up to 66KB
        val maxSearch = 0xFFFF + 22
        val start = (zip.size - 22).coerceAtLeast(0)
        val lowerBound = (zip.size - maxSearch).coerceAtLeast(0)
        for (i in start downTo lowerBound) {
            if (readUInt32(zip, i) == EOCD_SIG) return i
        }
        return null
    }

    private fun extractEntryData(zip: ByteArray, entry: InternalEntry): ByteArray? {
        if (entry.isDirectory) return ByteArray(0)
        val start = entry.dataOffset
        val compSize = entry.compressedSize
        if (start + compSize > zip.size) return null
        val slice = zip.copyOfRange(start, start + compSize)
        return when (entry.compressionMethod) {
            0 -> slice
            8 -> inflateDeflate(slice, expectedSize = if (entry.uncompressedSize > 0) entry.uncompressedSize else null)
            else -> null
        }
    }

    private fun gunzipDirect(data: ByteArray): ByteArray? = memScoped {
        val stream = alloc<z_stream>()
        stream.zalloc = null; stream.zfree = null; stream.opaque = null
        stream.next_in = null; stream.avail_in = 0u
        val rcInit = inflateInit2_(stream.ptr, GZIP_WINDOW_BITS, ZLIB_VERSION, sizeOf<z_stream>().convert())
        if (rcInit != Z_OK) {
            println("[Zip.iOS] gunzipDirect: inflateInit2 failed code=$rcInit")
            return null
        }
        try {
            var out = ByteArray(64 * 1024)
            var wrote = 0
            var iterations = 0
            data.usePinned { inPinned ->
                stream.next_in = inPinned.addressOf(0).reinterpret()
                stream.avail_in = data.size.convert()
                while (true) {
                    iterations++
                    if (iterations > 10_000) { // safety guard
                        println("[Zip.iOS] gunzipDirect: too many iterations, abort")
                        break
                    }
                    if (wrote == out.size) out = out.copyOf(out.size * 2)
                    out.usePinned { outPinned ->
                        val beforeOut = wrote
                        val beforeAvailIn = stream.avail_in.toInt()
                        stream.next_out = (outPinned.addressOf(0) + wrote).reinterpret()
                        stream.avail_out = (out.size - wrote).convert()
                        val rc = inflate(stream.ptr, Z_NO_FLUSH)
                        val produced = (out.size - wrote) - stream.avail_out.toInt()
                        wrote += produced
                        when (rc) {
                            Z_STREAM_END -> return@usePinned
                            Z_OK -> {
                                // If no progress and no more input, treat as end
                                if (produced == 0 && stream.avail_in.toInt() == 0) return@usePinned
                            }
                            Z_BUF_ERROR -> {
                                // No progress possible; if no input left -> done
                                if (stream.avail_in.toInt() == 0) return@usePinned else {
                                    println("[Zip.iOS] gunzipDirect: Z_BUF_ERROR with remaining input")
                                    return null
                                }
                            }
                            else -> {
                                println("[Zip.iOS] gunzipDirect: inflate error rc=$rc total=$wrote availIn=${stream.avail_in}")
                                return null
                            }
                        }
                        // Detect total stall
                        if (produced == 0 && stream.avail_in.toInt() == beforeAvailIn) {
                            if (stream.avail_in.toInt() == 0) return@usePinned else {
                                println("[Zip.iOS] gunzipDirect: stall with remaining input")
                                return null
                            }
                        }
                    }
                }
            }
            out.copyOf(wrote)
        } finally { inflateEnd(stream.ptr) }
    }

    private fun inflateDeflate(data: ByteArray, expectedSize: Int? = null): ByteArray? = memScoped {
        if (data.isEmpty()) return ByteArray(0)
        val stream = alloc<z_stream>()
        stream.zalloc = null; stream.zfree = null; stream.opaque = null
        val initCode = inflateInit2_(stream.ptr, RAW_DEFLATE_WINDOW_BITS, ZLIB_VERSION, sizeOf<z_stream>().convert())
        if (initCode != Z_OK) return null
        try {
            var outBuffer = ByteArray(expectedSize ?: (data.size * 2).coerceAtLeast(64))
            data.usePinned { inPinned ->
                stream.next_in = inPinned.addressOf(0).reinterpret()
                stream.avail_in = data.size.convert()
                while (true) {
                    if (stream.total_out.toInt() >= outBuffer.size) {
                        outBuffer = outBuffer.copyOf(outBuffer.size * 2)
                    }
                    outBuffer.usePinned { outPinned ->
                        stream.next_out = (outPinned.addressOf(0) + stream.total_out.toInt()).reinterpret()
                        stream.avail_out = (outBuffer.size - stream.total_out.toInt()).convert()
                        val rc = inflate(stream.ptr, Z_NO_FLUSH)
                        if (rc == Z_STREAM_END) return@usePinned
                        if (rc != Z_OK) return null
                        if (stream.avail_out.toInt() == 0) outBuffer = outBuffer.copyOf(outBuffer.size * 2)
                    }
                }
            }
            val produced = stream.total_out.toInt()
            outBuffer.copyOf(produced)
        } finally { inflateEnd(stream.ptr) }
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
}