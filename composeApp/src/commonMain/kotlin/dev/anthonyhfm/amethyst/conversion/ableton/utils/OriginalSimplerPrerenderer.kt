package dev.anthonyhfm.amethyst.conversion.ableton.utils

import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton.OriginalSimplerAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.data.MidiTrack
import dev.anthonyhfm.amethyst.conversion.ableton.data.OriginalSimpler
import dev.anthonyhfm.amethyst.conversion.ableton.utils.MidiChainReader
import dev.anthonyhfm.amethyst.core.engine.echo.Echo
import dev.anthonyhfm.amethyst.core.util.UUID
import dev.anthonyhfm.amethyst.core.util.randomUUID
import dev.anthonyhfm.amethyst.devices.audio.sample.SampleChainDeviceState
import dev.anthonyhfm.amethyst.timeline.data.AudioSource
import amethyst.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class OriginalSimplerPrerenderer {
    private data class FullAudio(
        val rawData: ByteArray,
        val sampleRate: Int,
        val channels: Int,
        val bitDepth: Int,
    )

    data class Result(
        val states: Map<OriginalSimplerAdapter.OriginalSimplerData, SampleChainDeviceState>,
        val sources: List<AudioSource>,
    )

    fun decodeAll(
        tracksList: List<MidiTrack>,
        reporter: dev.anthonyhfm.amethyst.core.loading.ProgressReporter? = null,
    ): Result {
        val simplers = tracksList
            .flatMap { MidiChainReader.getAllDevicesOfType<OriginalSimpler>(it) }
            .map { OriginalSimplerAdapter.getSimplerData(it) }

        if (simplers.isEmpty()) {
            val noSamplesMsg = runCatching { runBlocking { getString(Res.string.home_loading_no_samples_to_render) } }.getOrDefault("No audio samples to render")
            reporter?.update(1.0f, noSamplesMsg, detailText = null)
            return Result(emptyMap(), emptyList())
        }

        return runBlocking {
            val groupedByPath = simplers.groupBy { it.filePath }
            val total = groupedByPath.size
            val states = mutableMapOf<OriginalSimplerAdapter.OriginalSimplerData, SampleChainDeviceState>()
            val sources = mutableListOf<AudioSource>()

            val startingMsg = runCatching { getString(Res.string.home_loading_starting_audio_rendering, total.toString()) }.getOrDefault("Starting audio rendering ($total samples)...")
            reporter?.update(0f, startingMsg, detailText = null)

            groupedByPath.entries.forEachIndexed { index, (path, pathSimplers) ->
                val full = decodeFull(path)
                if (full != null) {
                    val source = AudioSource(
                        id = UUID.randomUUID(),
                        fileName = path.substringAfterLast('/').substringAfterLast('\\'),
                        rawData = full.rawData,
                        sampleRate = full.sampleRate,
                        channels = full.channels,
                        bitDepth = full.bitDepth,
                    )
                    sources += source
                    pathSimplers.forEach { simpler ->
                        states[simpler] = referenceRegion(
                            filePath = path,
                            source = source,
                            sampleStart = simpler.sampleStart,
                            sampleEnd = simpler.sampleEnd,
                        )
                    }
                }
                val count = index + 1
                val updateStep = (total / 50).coerceAtLeast(1)
                if (count == total || count == 1 || count % updateStep == 0) {
                    val fileName = path.substringAfterLast("/").substringAfterLast("\\")
                    val statusTextMsg = runCatching {
                        getString(Res.string.home_loading_rendering_sample, count.toString(), total.toString())
                    }.getOrDefault("Rendering audio sample $count of $total")
                    reporter?.update(count.toFloat() / total, statusTextMsg, fileName)
                }
            }
            Result(states, sources)
        }
    }

    private suspend fun decodeFull(filePath: String): FullAudio? = withContext(Dispatchers.IO) {
        val audioSignal = if (AbletonConverter.isZip) {
            val temporary = dev.anthonyhfm.amethyst.core.util.ConversionTempFiles.newPath(
                filePath.substringAfterLast('.', "wav")
            )
            try {
                if (AbletonConverter.extractZipEntryToFile(filePath, temporary)) {
                    Echo.decodeAudioFile(temporary)
                } else {
                    val bytes = readAudioFileBytes(filePath) ?: return@withContext null
                    Echo.decodeAudioData(bytes, filePath)
                }
            } finally {
                dev.anthonyhfm.amethyst.core.util.ConversionTempFiles.remove(temporary)
            }
        } else {
            Echo.decodeAudioFile(filePath)
        }

        if (audioSignal == null) {
            println("OriginalSimplerPrerenderer: error while decoding $filePath")
            return@withContext null
        }

        FullAudio(
            rawData = audioSignal.rawData ?: ByteArray(0),
            sampleRate = audioSignal.sampleRate,
            channels = audioSignal.channels,
            bitDepth = audioSignal.bitDepth,
        )
    }

    private fun readAudioFileBytes(filePath: String): ByteArray? {
        val bytes = AbletonConverter.readZipEntry(filePath)
        if (bytes == null) println("OriginalSimplerPrerenderer: file not found in archive: $filePath")
        return bytes
    }

    private fun referenceRegion(
        filePath: String,
        source: AudioSource,
        sampleStart: Long,
        sampleEnd: Long,
    ): SampleChainDeviceState {
        val startF = sampleStart.coerceAtLeast(0L)
            .coerceAtMost(source.totalSamples)
        val endRaw = if (sampleEnd <= 0L) source.totalSamples else sampleEnd
        val endF = endRaw.coerceIn(startF, source.totalSamples)

        if (startF >= endF) {
            return SampleChainDeviceState(
                fileName = filePath,
                sampleRate = source.sampleRate,
                channels = source.channels,
                bitDepth = source.bitDepth,
                totalDurationMs = source.totalDurationMs,
                sourceId = source.id,
                sourceStartFrame = startF,
                sourceEndFrameExclusive = endF,
                isLoaded = false,
            )
        }

        return SampleChainDeviceState(
            fileName = filePath,
            rawData = null,
            sampleRate = source.sampleRate,
            channels = source.channels,
            bitDepth = source.bitDepth,
            totalDurationMs = source.totalDurationMs,
            isLoaded = true,
            sourceId = source.id,
            startPosition = startF.toDouble().div(source.totalSamples).toFloat(),
            endPosition = endF.toDouble().div(source.totalSamples).toFloat(),
            sourceStartFrame = startF,
            sourceEndFrameExclusive = endF,
        )
    }

}
