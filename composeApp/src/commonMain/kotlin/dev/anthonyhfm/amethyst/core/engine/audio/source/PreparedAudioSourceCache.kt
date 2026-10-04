package dev.anthonyhfm.amethyst.core.engine.audio.source

import kotlinx.atomicfu.atomic

/**
 * Small process-local cache of PCM sources prepared at the active hardware rate.
 *
 * Preparation is a control-thread operation. Returned sources are immutable and
 * can be read directly by the realtime renderer without sample-rate conversion.
 */
object PreparedAudioSourceCache {
    private data class Key(
        val id: String,
        val sourceRate: Int,
        val outputRate: Int,
        val channels: Int,
        val frameCount: Long,
    )

    private data class Entry(
        val sourceBytes: ByteArray?,
        val prepared: AudioSource,
        val retainForProject: Boolean,
    )

    private val entries = atomic<Map<Key, Entry>>(emptyMap())
    private val persistentRoot = atomic<String?>(null)

    /** A converted mobile project keeps prepared PCM for later offline opens. */
    fun configurePersistentRoot(root: String?) {
        if (persistentRoot.value != root) {
            entries.value = emptyMap()
            persistentRoot.value = root
        }
    }

    fun getOrPrepare(
        source: AudioSource,
        outputRate: Int,
        retainForProject: Boolean = false,
    ): AudioSource {
        require(outputRate > 0)
        if (source.sampleRate == outputRate) {
            return source
        }
        val key = Key(
            id = source.id,
            sourceRate = source.sampleRate,
            outputRate = outputRate,
            channels = source.channels,
            frameCount = source.frameCount,
        )
        val sourceBytes = (source as? ByteArrayPcmAudioSource)?.rawData
        cachedSource(key = key, sourceBytes = sourceBytes, retainForProject = retainForProject)?.let {
            return it
        }

        val diskKey = (source as? ByteArrayPcmAudioSource)?.let {
            "${source.id.hashCode().toUInt().toString(16)}-${it.rawData.contentHashCode().toUInt().toString(16)}-" +
                "${source.sampleRate}-$outputRate-${source.channels}-${source.frameCount}"
        }
        val outputFrames = (source.frameCount.toDouble() * outputRate / source.sampleRate)
            .toLong().coerceAtLeast(1L)
        val expectedBytes = outputFrames * source.channels * BYTES_PER_PCM24_SAMPLE
        val root = persistentRoot.value
        val diskBytes = if (root != null && diskKey != null && expectedBytes <= Int.MAX_VALUE) {
            runCatching { PreparedAudioDiskCache.read(root, diskKey, expectedBytes.toInt()) }.getOrNull()
        } else null
        val prepared = if (diskBytes != null) {
            ByteArrayPcmAudioSource(source.id, outputRate, source.channels, 24, diskBytes)
        } else {
            (if (source is ByteArrayPcmAudioSource) {
                platformResampleToPcm24(source, outputRate)
            } else null).let { it ?: resampleToPcm24(source, outputRate) }.also { result ->
                if (root != null && diskKey != null && result is ByteArrayPcmAudioSource) {
                    runCatching { PreparedAudioDiskCache.write(root, diskKey, result.rawData) }
                }
            }
        }
        while (true) {
            val current = entries.value
            val existing = current[key]?.takeIf { it.sourceBytes === sourceBytes }
            if (existing != null) {
                if (!retainForProject || existing.retainForProject) {
                    return existing.prepared
                }
                if (
                    entries.compareAndSet(
                        expect = current,
                        update = current + (key to existing.copy(retainForProject = true)),
                    )
                ) {
                    return existing.prepared
                }
                continue
            }
            val updated = current + (key to Entry(
                sourceBytes = sourceBytes,
                prepared = prepared,
                retainForProject = retainForProject,
            ))
            val transientEntries = updated.entries.filter { !it.value.retainForProject }
            val evictedKeys = transientEntries
                .take((transientEntries.size - MAXIMUM_ENTRIES).coerceAtLeast(0))
                .map { it.key }
            if (entries.compareAndSet(expect = current, update = updated - evictedKeys.toSet())) {
                return prepared
            }
        }
    }

    private fun cachedSource(
        key: Key,
        sourceBytes: ByteArray?,
        retainForProject: Boolean,
    ): AudioSource? {
        while (true) {
            val current = entries.value
            val existing = current[key]?.takeIf { it.sourceBytes === sourceBytes } ?: return null
            if (!retainForProject || existing.retainForProject) {
                return existing.prepared
            }
            if (
                entries.compareAndSet(
                    expect = current,
                    update = current + (key to existing.copy(retainForProject = true)),
                )
            ) {
                return existing.prepared
            }
        }
    }

    internal fun retainedPcmBytes(): Long = entries.value.values
        .mapNotNull { (it.prepared as? ByteArrayPcmAudioSource)?.rawData }
        .toSet()
        .sumOf { it.size.toLong() }

    internal fun removeSources(sourceIds: Set<String>) {
        while (true) {
            val current = entries.value
            val updated = current.filterKeys { it.id !in sourceIds }
            if (entries.compareAndSet(expect = current, update = updated)) {
                return
            }
        }
    }

    fun clear() {
        entries.value = emptyMap()
    }

    private fun resampleToPcm24(source: AudioSource, outputRate: Int): AudioSource {
        val outputFrames = (
            source.frameCount.toDouble() * outputRate / source.sampleRate
            ).toLong().coerceAtLeast(1L)
        require(outputFrames <= Int.MAX_VALUE / (source.channels * BYTES_PER_PCM24_SAMPLE)) {
            "Prepared audio source is too large"
        }
        val output = ByteArray(
            outputFrames.toInt() * source.channels * BYTES_PER_PCM24_SAMPLE,
        )
        val frame = FloatArray(source.channels)
        val resampler = PolyphaseSincResampler(
            sourceRate = source.sampleRate,
            outputRate = outputRate,
            channels = source.channels,
        )
        var outputFrame = 0
        while (outputFrame < outputFrames.toInt()) {
            resampler.readFrame(source, frame)
            var channel = 0
            while (channel < source.channels) {
                writePcm24(
                    destination = output,
                    sampleIndex = outputFrame * source.channels + channel,
                    sample = frame[channel],
                )
                channel++
            }
            resampler.advance()
            outputFrame++
        }
        return ByteArrayPcmAudioSource(
            id = source.id,
            sampleRate = outputRate,
            channels = source.channels,
            bitDepth = 24,
            rawData = output,
        )
    }

    private fun writePcm24(
        destination: ByteArray,
        sampleIndex: Int,
        sample: Float,
    ) {
        val normalized = sample.takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f
        val value = if (normalized <= -1f) {
            -8_388_608
        } else {
            (normalized * 8_388_607f).toInt()
        }
        val offset = sampleIndex * BYTES_PER_PCM24_SAMPLE
        destination[offset] = (value and 0xff).toByte()
        destination[offset + 1] = ((value ushr 8) and 0xff).toByte()
        destination[offset + 2] = ((value ushr 16) and 0xff).toByte()
    }

    private const val MAXIMUM_ENTRIES = 64
    private const val BYTES_PER_PCM24_SAMPLE = 3
}
