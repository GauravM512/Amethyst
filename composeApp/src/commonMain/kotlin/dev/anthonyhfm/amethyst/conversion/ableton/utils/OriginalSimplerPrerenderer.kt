package dev.anthonyhfm.amethyst.conversion.ableton.utils

import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton.OriginalSimplerAdapter
import dev.anthonyhfm.amethyst.core.engine.echo.AudioDecoder
import dev.anthonyhfm.amethyst.devices.audio.clip.ClipChainDeviceState
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class OriginalSimplerPrerenderer {
    // Vollständig decodierte PCM-Daten Cache (pro Datei nur einmal decodieren)
    private data class FullAudio(
        val rawData: ByteArray,
        val sampleRate: Int,
        val channels: Int,
        val bitDepth: Int,
        val totalFrames: Long // Anzahl PCM Frames (nicht Samples * Channels)
    )

    private val fullDecodeCache = mutableMapOf<String, Deferred<FullAudio?>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun clearCache() {
        fullDecodeCache.clear()
    }

    fun decodeAll(tracksList: List<XmlElement>): Map<OriginalSimplerAdapter.OriginalSimplerData, ClipChainDeviceState> {
        val simplers = tracksList
            .flatMap { it.querySelector("OriginalSimpler") }
            .mapNotNull { OriginalSimplerAdapter.getSimplerData(it) }
        if (simplers.isEmpty()) return emptyMap()

        val limitedIO = Dispatchers.Default.limitedParallelism(8)
        val gate = Semaphore(8)

        return runBlocking {
            // Einmalige Decodes pro Datei vorbereiten
            val uniquePaths = simplers.map { it.filePath }.distinct()

            uniquePaths.forEach { path ->
                if (!fullDecodeCache.containsKey(path)) {
                    fullDecodeCache[path] = scope.async(limitedIO) {
                        gate.withPermit { decodeFull(path) }
                    }
                }
            }

            // Sicherstellen dass alle Decodes abgeschlossen sind
            val decodedMap: Map<String, FullAudio?> = fullDecodeCache
                .filter { it.key in uniquePaths }
                .mapValues { (_, deferred) -> deferred.await() }

            // Für jeden Simpler entsprechenden Ausschnitt erstellen
            simplers.associateWith { simpler ->
                val full = decodedMap[simpler.filePath]
                if (full == null) {
                    ClipChainDeviceState() // Fehler beim Decodieren der Datei
                } else {
                    sliceSegment(
                        filePath = simpler.filePath,
                        full = full,
                        sampleStart = simpler.sampleStart,
                        sampleEnd = simpler.sampleEnd
                    )
                }
            }
        }
    }

    private suspend fun decodeFull(filePath: String): FullAudio? = withContext(Dispatchers.IO) {
        val audioFileBytes: ByteArray = if (AbletonConverter.isZip) {
            val fileBytes = AbletonConverter.zipEntries[filePath]?.data
            if (fileBytes == null) {
                println("OriginalSimplerPrerenderer: Datei im ZIP nicht gefunden: $filePath")
                return@withContext null
            }
            fileBytes
        } else {
            val audioFile = PlatformFile(filePath)
            if (!audioFile.exists() || !audioFile.isRegularFile()) {
                println("OriginalSimplerPrerenderer: Datei nicht vorhanden: $filePath")
                return@withContext null
            }
            audioFile.readBytes()
        }

        val audioSignal = AudioDecoder.decodeAudioData(
            audioData = audioFileBytes,
            fileName = filePath,
            sampleStart = null,
            sampleEnd = null
        )

        if (audioSignal == null) {
            println("OriginalSimplerPrerenderer: Decoding fehlgeschlagen für $filePath")
            return@withContext null
        }

        if (AbletonConverter.isZip) {
            AbletonConverter.zipEntries.remove(filePath)
        }

        val frameSizeBytes = (audioSignal.channels * (audioSignal.bitDepth / 8))
        val totalFrames = if (frameSizeBytes > 0) (audioSignal.rawData?.size ?: 0) / frameSizeBytes else 0

        FullAudio(
            rawData = audioSignal.rawData ?: ByteArray(0),
            sampleRate = audioSignal.sampleRate,
            channels = audioSignal.channels,
            bitDepth = audioSignal.bitDepth,
            totalFrames = totalFrames.toLong()
        )
    }

    private fun sliceSegment(
        filePath: String,
        full: FullAudio,
        sampleStart: Long,
        sampleEnd: Long
    ): ClipChainDeviceState {
        val frameSize = full.channels * (full.bitDepth / 8)
        if (frameSize <= 0) return ClipChainDeviceState(fileName = filePath, isLoaded = false)

        val startF = sampleStart.coerceAtLeast(0L)
        val endRaw = if (sampleEnd <= 0L) full.totalFrames else sampleEnd
        val endF = endRaw.coerceAtMost(full.totalFrames)

        if (startF >= endF) {
            return ClipChainDeviceState(
                fileName = filePath,
                rawData = ByteArray(0),
                sampleRate = full.sampleRate,
                channels = full.channels,
                bitDepth = full.bitDepth,
                isLoaded = false
            )
        }

        val startByte = (startF * frameSize).toInt()
        val endByte = (endF * frameSize).toInt()
        if (startByte >= full.rawData.size) {
            return ClipChainDeviceState(fileName = filePath, isLoaded = false)
        }
        val safeEndByte = endByte.coerceAtMost(full.rawData.size)
        val slice = full.rawData.copyOfRange(startByte, safeEndByte)

        return ClipChainDeviceState(
            fileName = filePath,
            rawData = slice,
            sampleRate = full.sampleRate,
            channels = full.channels,
            bitDepth = full.bitDepth,
            isLoaded = true
        )
    }
}