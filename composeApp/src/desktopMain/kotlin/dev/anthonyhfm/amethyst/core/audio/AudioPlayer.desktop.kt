package dev.anthonyhfm.amethyst.core.audio

import dev.anthonyhfm.amethyst.core.util.UUID
import dev.anthonyhfm.amethyst.core.util.randomUUID
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

actual object AudioPlayer {
    private var isInitialized = false
    private val audioClips = ConcurrentHashMap<String, ReadyAudioClip>()
    private val activePlayers = ConcurrentHashMap<String, PlaybackInstance>()

    // High-performance audio system
    private var mixer: Mixer? = null
    private lateinit var targetFormat: AudioFormat
    private val playbackExecutor = Executors.newCachedThreadPool { r ->
        Thread(r, "AudioPlayer-Instant").apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY - 1
        }
    }

    // Professional audio settings (Ableton Live standard)
    private val bufferSize = 512 // 512 samples = ~11.6ms latency @ 44.1kHz (Ableton standard)
    private val maxConcurrentPlayers = 64
    private val playbackCounter = AtomicInteger(0)

    private data class ReadyAudioClip(
        val pcmData: ByteArray,
        val duration: Long,
        val format: AudioFormat
    )

    private class PlaybackInstance(
        private val line: SourceDataLine,
        private val data: ByteArray
    ) {
        @Volatile private var isPlaying = false
        @Volatile private var shouldStop = false

        fun play() {
            if (isPlaying) return
            isPlaying = true

            line.start()

            // Stream data in kleine Chunks für minimale Latenz
            var offset = 0
            val chunkSize = 512 // Sehr kleine Chunks

            while (offset < data.size && !shouldStop) {
                val remaining = data.size - offset
                val writeSize = min(chunkSize, remaining)

                try {
                    val written = line.write(data, offset, writeSize)
                    offset += written

                    // Micro-pause to prevent CPU hogging while maintaining low latency
                    if (written < writeSize) {
                        Thread.sleep(0, 100) // 0.1ms
                    }
                } catch (e: Exception) {
                    break
                }
            }

            line.drain()
            line.stop()
            isPlaying = false
        }

        fun stop() {
            shouldStop = true
            if (isPlaying) {
                line.stop()
                line.flush()
            }
        }

        fun close() {
            stop()
            line.close()
        }

        fun isActive(): Boolean = isPlaying
    }

    private fun ensureInitialized() {
        if (!isInitialized) {
            try {
                initializeAudioSystem()
            } catch (e: Exception) {
                println("Critical: Failed to initialize high-performance audio system: ${e.message}")
                throw RuntimeException("Audio system initialization failed.", e)
            }
        }
    }

    private fun initializeAudioSystem() {
        // Find the best mixer for low-latency audio
        val mixerInfos = AudioSystem.getMixerInfo()

        mixer = mixerInfos.find { info ->
            val mixer = AudioSystem.getMixer(info)
            try {
                // Test if mixer supports our target format
                val testFormat = AudioFormat(44100f, 16, 2, true, false)
                val lineInfo = DataLine.Info(SourceDataLine::class.java, testFormat)
                mixer.isLineSupported(lineInfo) && (
                    info.name.contains("DirectSound", ignoreCase = true) ||
                    info.name.contains("CoreAudio", ignoreCase = true) ||
                    info.name.contains("Primary", ignoreCase = true) ||
                    info.name.contains("Default", ignoreCase = true)
                )
            } catch (e: Exception) {
                false
            }
        }?.let { AudioSystem.getMixer(it) } ?: AudioSystem.getMixer(null)

        // Standard-Format für minimale Konvertierung
        targetFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            44100f, // Standard sample rate
            16,     // 16-bit für gute Performance/Quality balance
            2,      // Stereo
            4,      // 2 channels * 2 bytes
            44100f,
            false   // Little endian
        )

        isInitialized = true
        println("High-performance audio system initialized:")
        println("  Mixer: ${mixer?.mixerInfo?.name ?: "Default"}")
        println("  Format: ${targetFormat.sampleRate.toInt()}Hz, ${targetFormat.sampleSizeInBits}bit, ${targetFormat.channels}ch")
        println("  Buffer size: ${bufferSize} samples")
    }

    private fun createReadyToPlayLine(): SourceDataLine? {
        return try {
            val lineInfo = DataLine.Info(SourceDataLine::class.java, targetFormat)
            val line = (mixer?.getLine(lineInfo) ?: AudioSystem.getLine(lineInfo)) as SourceDataLine
            line.open(targetFormat, bufferSize * targetFormat.frameSize)
            line
        } catch (e: Exception) {
            println("Failed to create audio line: ${e.message}")
            null
        }
    }

    actual fun loadAudio(data: ByteArray, uuid: String?): String {
        ensureInitialized()
        val audioId = uuid ?: UUID.randomUUID()

        try {
            val audioInfo = decodeAudioData(data)

            // Convert to target format for immediate playback
            val convertedData = convertToTargetFormat(audioInfo)

            audioClips[audioId] = ReadyAudioClip(
                pcmData = convertedData.pcmData,
                duration = convertedData.duration,
                format = targetFormat
            )

            val clip = AudioClip(
                name = "Audio_${audioId.take(8)}",
                length = convertedData.duration,
                data = data,
                key = audioId
            )
            WorkspaceRepository.audioRegistry[audioId] = clip

            println("Audio ready for instant playback: $audioId (${convertedData.duration}ms)")
            return audioId
        } catch (e: Exception) {
            audioClips.remove(audioId)
            println("Failed to prepare audio for instant playback: ${e.message}")
            throw e
        }
    }

    private fun convertToTargetFormat(audioInfo: AudioInfo): AudioInfo {
        // Check if formats are compatible
        if (audioInfo.format.encoding == targetFormat.encoding &&
            audioInfo.format.sampleRate == targetFormat.sampleRate &&
            audioInfo.format.sampleSizeInBits == targetFormat.sampleSizeInBits &&
            audioInfo.format.channels == targetFormat.channels &&
            audioInfo.format.isBigEndian == targetFormat.isBigEndian) {
            return audioInfo
        }

        return try {
            val sourceStream = AudioInputStream(
                ByteArrayInputStream(audioInfo.pcmData),
                audioInfo.format,
                audioInfo.pcmData.size / audioInfo.format.frameSize.toLong()
            )

            val convertedStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream)
            val convertedData = convertedStream.readAllBytes()

            val frameLength = convertedData.size / targetFormat.frameSize
            val duration = (frameLength * 1000L / targetFormat.sampleRate).toLong()

            convertedStream.close()
            sourceStream.close()

            AudioInfo(targetFormat, convertedData, duration)
        } catch (e: Exception) {
            println("Audio format conversion failed, using original: ${e.message}")
            audioInfo
        }
    }

    actual fun playAudio(audioKey: String) {
        ensureInitialized()
        val readyClip = audioClips[audioKey] ?: run {
            println("Audio clip not found or not ready: $audioKey")
            return
        }

        if (playbackCounter.get() >= maxConcurrentPlayers) {
            println("Max concurrent players reached ($maxConcurrentPlayers)")
            return
        }

        // Immediate playback - no delays, no blocking operations
        playbackExecutor.submit {
            val line = createReadyToPlayLine() ?: return@submit

            val playId = "${audioKey}_${System.nanoTime()}"
            val player = PlaybackInstance(line, readyClip.pcmData)

            activePlayers[playId] = player
            playbackCounter.incrementAndGet()

            try {
                player.play() // This starts immediately
            } finally {
                activePlayers.remove(playId)
                playbackCounter.decrementAndGet()
                player.close()
            }
        }
    }

    actual fun stopAudio(audioKey: String) {
        ensureInitialized()

        // Immediate stop - no executor needed
        activePlayers.entries.removeAll { (playId, player) ->
            if (playId.startsWith(audioKey)) {
                player.stop()
                true
            } else false
        }
    }

    actual fun getAudioClip(data: ByteArray): AudioClip? {
        ensureInitialized()
        val audioId = UUID.randomUUID()

        return try {
            val audioInfo = decodeAudioData(data)
            AudioClip(
                name = "Audio_${audioId.take(8)}",
                length = audioInfo.duration,
                data = data,
                key = audioId
            )
        } catch (e: Exception) {
            println("Failed to create audio clip: ${e.message}")
            null
        }
    }

    actual fun getAudioClip(data: ByteArray, sampleStart: Long, sampleEnd: Long): AudioClip? {
        ensureInitialized()
        val audioId = UUID.randomUUID()

        return try {
            val audioInfo = decodeAudioData(data)

            val totalSamples = audioInfo.pcmData.size / audioInfo.format.frameSize
            if (sampleStart < 0 || sampleEnd <= sampleStart || sampleEnd > totalSamples) {
                println("Invalid sample range: start=$sampleStart, end=$sampleEnd, totalSamples=$totalSamples")
                return null
            }

            val frameSize = audioInfo.format.frameSize
            val startByte = (sampleStart * frameSize).toInt()
            val endByte = (sampleEnd * frameSize).toInt()

            val extractedPcmData = audioInfo.pcmData.copyOfRange(startByte, endByte)
            val extractedWavData = createWavFromPcm(extractedPcmData, audioInfo.format)

            val extractedSamples = sampleEnd - sampleStart
            val extractedDuration = (extractedSamples * 1000L) / audioInfo.format.sampleRate.toLong()

            AudioClip(
                name = "Sample_${audioId.take(8)}_${sampleStart}-${sampleEnd}",
                length = extractedDuration,
                data = extractedWavData,
                key = audioId
            )
        } catch (e: Exception) {
            println("Failed to extract audio sample: ${e.message}")
            null
        }
    }

    actual fun preloadFromAudioClip(audioClip: AudioClip) {
        loadAudio(audioClip.data, audioClip.key)
    }

    fun removeAudio(audioKey: String) {
        audioClips.remove(audioKey)
        WorkspaceRepository.audioRegistry.remove(audioKey)
        stopAudio(audioKey)
    }

    // ...existing decode methods...
    private fun decodeAudioData(data: ByteArray): AudioInfo = when {
        isWav(data) -> decodeWav(data)
        isMp3(data) -> decodeMp3(data)
        isOgg(data) -> throw RuntimeException("OGG format not supported in Java Sound implementation. Please use WAV or MP3 files.")
        else -> throw RuntimeException("Unsupported audio format. Only WAV and MP3 files are supported.")
    }

    private fun isWav(d: ByteArray) = d.size >= 12 &&
        d[0] == 'R'.code.toByte() && d[1] == 'I'.code.toByte() &&
        d[2] == 'F'.code.toByte() && d[3] == 'F'.code.toByte()

    private fun isMp3(d: ByteArray) = d.size >= 3 && (
        (d[0] == 'I'.code.toByte() && d[1] == 'D'.code.toByte() && d[2] == '3'.code.toByte()) ||
        (d[0] == 0xFF.toByte() && (d[1].toInt() and 0xE0) == 0xE0) ||
        d.indices.take(100).any { i ->
            i < d.size - 1 && d[i] == 0xFF.toByte() && (d[i + 1].toInt() and 0xE0) == 0xE0
        }
    )

    private fun isOgg(d: ByteArray) = d.size >= 4 &&
        d[0] == 'O'.code.toByte() && d[1] == 'g'.code.toByte() &&
        d[2] == 'g'.code.toByte() && d[3] == 'S'.code.toByte()

    private fun decodeWav(audioData: ByteArray): AudioInfo {
        val buf = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN)
        require(buf.limit() >= 44) { "Invalid WAV: too short" }

        val riffHeader = ByteArray(4).also { buf.get(it) }
        require(String(riffHeader, Charsets.US_ASCII) == "RIFF") { "Not a valid RIFF file" }

        buf.int // Skip file size

        val waveHeader = ByteArray(4).also { buf.get(it) }
        require(String(waveHeader, Charsets.US_ASCII) == "WAVE") { "Not a valid WAVE file" }

        var channels = 0
        var sampleRate = 0
        var bitsPerSample = 0
        var dataSize = 0
        var dataStartPos = 0
        var fmtFound = false

        while (buf.remaining() >= 8) {
            val chunkId = ByteArray(4).also { buf.get(it) }
            val chunkSize = buf.int
            val chunkIdStr = String(chunkId, Charsets.US_ASCII)

            when (chunkIdStr) {
                "fmt " -> {
                    val chunkStart = buf.position()
                    val audioFormat = buf.short.toInt()
                    require(audioFormat == 1) { "Only PCM format is supported, got format: $audioFormat" }

                    channels = buf.short.toInt()
                    sampleRate = buf.int
                    buf.int // Skip byte rate
                    buf.short // Skip block align
                    bitsPerSample = buf.short.toInt()

                    fmtFound = true
                    buf.position(chunkStart + chunkSize)
                }
                "data" -> {
                    require(fmtFound) { "fmt chunk must come before data chunk" }
                    dataSize = chunkSize
                    dataStartPos = buf.position()
                    break
                }
                else -> {
                    buf.position(buf.position() + chunkSize)
                }
            }
        }

        require(fmtFound) { "No fmt chunk found in WAV file" }
        require(dataSize > 0) { "No data chunk found in WAV file" }
        require(bitsPerSample in listOf(8, 16, 24, 32)) { "Unsupported bit depth: $bitsPerSample" }
        require(channels in 1..2) { "Unsupported channel count: $channels" }

        val encoding = if (bitsPerSample == 8) AudioFormat.Encoding.PCM_UNSIGNED else AudioFormat.Encoding.PCM_SIGNED
        val frameSize = channels * (bitsPerSample / 8)
        val frameRate = sampleRate.toFloat()

        val audioFormat = AudioFormat(encoding, frameRate, bitsPerSample, channels, frameSize, frameRate, false)

        buf.position(dataStartPos)
        val audioBytes = ByteArray(dataSize)
        buf.get(audioBytes, 0, minOf(dataSize, buf.remaining()))

        val samples = dataSize / frameSize
        val duration = (samples * 1000L) / sampleRate

        return AudioInfo(audioFormat, audioBytes, duration)
    }

    private fun decodeMp3(audioData: ByteArray): AudioInfo {
        try {
            val inputStream = ByteArrayInputStream(audioData)
            val audioInputStream = AudioSystem.getAudioInputStream(inputStream)

            val sourceFormat = audioInputStream.format

            val targetFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.sampleRate,
                16,
                sourceFormat.channels,
                sourceFormat.channels * 2,
                sourceFormat.sampleRate,
                false
            )

            val pcmStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream)
            val pcmData = pcmStream.readAllBytes()

            val frameLength = pcmData.size / targetFormat.frameSize
            val duration = (frameLength * 1000L / targetFormat.sampleRate).toLong()

            pcmStream.close()
            audioInputStream.close()

            return AudioInfo(targetFormat, pcmData, duration)

        } catch (e: Exception) {
            throw RuntimeException("Failed to decode MP3: ${e.message}", e)
        }
    }

    fun cleanup() {
        if (!isInitialized) return

        // Stop all active players immediately
        activePlayers.values.forEach { it.stop() }
        activePlayers.clear()

        audioClips.clear()

        playbackExecutor.shutdown()
        try {
            if (!playbackExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                playbackExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            playbackExecutor.shutdownNow()
        }

        isInitialized = false
        println("High-performance audio system cleaned up")
    }
}

private data class AudioInfo(
    val format: AudioFormat,
    val pcmData: ByteArray,
    val duration: Long
)

private fun createWavFromPcm(pcmData: ByteArray, audioFormat: AudioFormat): ByteArray {
    val byteArrayOutputStream = java.io.ByteArrayOutputStream()

    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(36 + pcmData.size)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16)
    header.putShort(1)
    header.putShort(audioFormat.channels.toShort())
    header.putInt(audioFormat.sampleRate.toInt())
    header.putInt(audioFormat.sampleRate.toInt() * audioFormat.channels * (audioFormat.sampleSizeInBits / 8))
    header.putShort((audioFormat.channels * (audioFormat.sampleSizeInBits / 8)).toShort())
    header.putShort(audioFormat.sampleSizeInBits.toShort())
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(pcmData.size)

    byteArrayOutputStream.write(header.array())
    byteArrayOutputStream.write(pcmData)

    return byteArrayOutputStream.toByteArray()
}
