package dev.anthonyhfm.amethyst.core.engine.echo

import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import kotlinx.coroutines.*
import org.lwjgl.openal.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

actual object AudioOutput {
    private var device: Long = 0L
    private var context: Long = 0L
    private val activeSources = ConcurrentHashMap<String, AudioSource>()
    private var isInitialized = false
    private var processingJob: Job? = null
    private val audioQueue = ConcurrentLinkedQueue<QueuedAudio>()

    private const val SAMPLE_RATE = 44100
    private const val FORMAT = AL10.AL_FORMAT_STEREO16
    private const val MAX_SOURCES = 16
    private const val BUFFER_SIZE_SAMPLES = 512 // 512 samples = ~11.6ms bei 44.1kHz
    private const val BUFFER_SIZE_BYTES = BUFFER_SIZE_SAMPLES * 2 * 2 // 16-bit stereo = 2048 bytes
    private const val BUFFERS_PER_SOURCE = 4 // Anzahl der Buffers pro Source für Streaming

    data class QueuedAudio(
        val pcmData: ByteArray,
        val audioKey: String?,
        val origin: Any?
    )

    data class AudioSource(
        val sourceId: Int,
        val bufferIds: IntArray,
        val audioKey: String?,
        val origin: Any?,
        val audioData: ByteArray,
        var currentPosition: Int,
        val isStreaming: Boolean
    ) {
        fun cleanup() {
            try {
                AL10.alSourceStop(sourceId)
                AL10.alDeleteSources(sourceId)
                AL10.alDeleteBuffers(bufferIds)
            } catch (e: Exception) {
                println("Error during AudioSource cleanup: ${e.message}")
            }
        }
    }

    init {
        initializeOpenAL()
    }

    private fun initializeOpenAL() {
        try {
            println("Initializing OpenAL with LWJGL...")

            try {
                System.loadLibrary("lwjgl")
            } catch (e: UnsatisfiedLinkError) {
                println("LWJGL native library loading failed, trying alternative method...")
            }

            device = ALC10.alcOpenDevice(null as ByteBuffer?)
            if (device == 0L) {
                println("Failed to open OpenAL device")
                return
            }

            context = ALC10.alcCreateContext(device, null as IntArray?)
            if (context == 0L) {
                println("Failed to create OpenAL context")
                ALC10.alcCloseDevice(device)
                return
            }

            if (!ALC10.alcMakeContextCurrent(context)) {
                println("Failed to make OpenAL context current")
                ALC10.alcDestroyContext(context)
                ALC10.alcCloseDevice(device)
                return
            }

            val alcCapabilities = ALC.createCapabilities(device)
            val alCapabilities = AL.createCapabilities(alcCapabilities)

            if (!alCapabilities.OpenAL10) {
                println("OpenAL 1.0 not supported")
                cleanup()
                return
            }

            isInitialized = true
            println("OpenAL Audio Output initialized successfully with 512-sample buffers")

            startAudioProcessing()

        } catch (e: Exception) {
            println("OpenAL initialization failed: ${e.message}")
            e.printStackTrace()
            cleanup()
        }
    }

    private fun startAudioProcessing() {
        processingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isInitialized) {
                try {
                    val hadWork = processAudioQueue() || updateStreamingSources()

                    // Adaptive delay: nur bei Idle-Zeit delay
                    if (!hadWork) {
                        delay(1)
                    }
                } catch (e: Exception) {
                    println("Audio processing error: ${e.message}")
                    delay(1)
                }
            }
        }
    }

    private fun processAudioQueue(): Boolean {
        if (!isInitialized) return false

        var hadWork = false
        repeat(8) {
            val queuedAudio = audioQueue.poll() ?: return hadWork
            createAndPlayAudioSource(queuedAudio)
            hadWork = true
        }
        return hadWork
    }

    private fun updateStreamingSources(): Boolean {
        var hadWork = false
        activeSources.values.forEach { audioSource ->
            if (audioSource.isStreaming) {
                val state = AL10.alGetSourcei(audioSource.sourceId, AL10.AL_SOURCE_STATE)
                if (state == AL10.AL_PLAYING) {
                    if (refillStreamingBuffers(audioSource)) {
                        hadWork = true
                    }
                }
            }
        }
        cleanupFinishedSources()
        return hadWork
    }

    private fun createAndPlayAudioSource(queuedAudio: QueuedAudio) {
        try {
            if (activeSources.size >= MAX_SOURCES) {
                println("Too many active audio sources, skipping")
                return
            }

            val sourceId = AL10.alGenSources()
            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                println("Failed to create OpenAL source")
                return
            }

            val bufferIds = IntArray(BUFFERS_PER_SOURCE)
            AL10.alGenBuffers(bufferIds)
            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                println("Failed to create OpenAL buffers")
                AL10.alDeleteSources(sourceId)
                return
            }

            AL10.alSourcef(sourceId, AL10.AL_PITCH, 1.0f)
            AL10.alSourcef(sourceId, AL10.AL_GAIN, 1.0f)
            AL10.alSource3f(sourceId, AL10.AL_POSITION, 0.0f, 0.0f, 0.0f)
            AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE)

            val pcmData = queuedAudio.pcmData
            val validSize = (pcmData.size / 4) * 4
            val validData = if (validSize != pcmData.size) {
                pcmData.sliceArray(0 until validSize)
            } else {
                pcmData
            }

            val audioSource = AudioSource(
                sourceId = sourceId,
                bufferIds = bufferIds,
                audioKey = queuedAudio.audioKey,
                origin = queuedAudio.origin,
                audioData = validData,
                currentPosition = 0,
                isStreaming = validData.size > BUFFER_SIZE_BYTES * 2
            )

            val key = generateSourceKey(queuedAudio.audioKey, queuedAudio.origin)
            activeSources[key]?.cleanup()
            activeSources[key] = audioSource

            if (audioSource.isStreaming) {
                fillInitialBuffers(audioSource)
                println("Started streaming audio with ${audioSource.audioData.size} bytes in 512-sample chunks")
            } else {
                fillSingleBuffer(audioSource)
                println("Started single-buffer audio with ${audioSource.audioData.size} bytes")
            }

            AL10.alSourcePlay(sourceId)

            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                println("Failed to start audio playback")
                audioSource.cleanup()
                activeSources.remove(key)
            }

        } catch (e: Exception) {
            println("Error creating audio source: ${e.message}")
        }
    }

    private fun fillInitialBuffers(audioSource: AudioSource) {
        for (i in 0 until minOf(BUFFERS_PER_SOURCE - 1, 2)) {
            if (audioSource.currentPosition < audioSource.audioData.size) {
                fillNextBuffer(audioSource, audioSource.bufferIds[i])
            }
        }
    }

    private fun fillSingleBuffer(audioSource: AudioSource) {
        val buffer = MemoryUtil.memAlloc(audioSource.audioData.size)
        buffer.put(audioSource.audioData)
        buffer.flip()

        AL10.alBufferData(audioSource.bufferIds[0], FORMAT, buffer, SAMPLE_RATE)
        MemoryUtil.memFree(buffer)

        AL10.alSourceQueueBuffers(audioSource.sourceId, audioSource.bufferIds[0])
    }

    private fun fillNextBuffer(audioSource: AudioSource, bufferId: Int): Boolean {
        val remainingBytes = audioSource.audioData.size - audioSource.currentPosition
        if (remainingBytes <= 0) return false

        val chunkSize = minOf(BUFFER_SIZE_BYTES, remainingBytes)
        val endPosition = audioSource.currentPosition + chunkSize

        val chunk = audioSource.audioData.sliceArray(
            audioSource.currentPosition until endPosition
        )

        val buffer = MemoryUtil.memAlloc(chunk.size)
        buffer.put(chunk)
        buffer.flip()

        AL10.alBufferData(bufferId, FORMAT, buffer, SAMPLE_RATE)
        MemoryUtil.memFree(buffer)

        AL10.alSourceQueueBuffers(audioSource.sourceId, bufferId)
        audioSource.currentPosition = endPosition

        return true
    }

    private fun refillStreamingBuffers(audioSource: AudioSource): Boolean {
        val processed = AL10.alGetSourcei(audioSource.sourceId, AL10.AL_BUFFERS_PROCESSED)

        if (processed > 0) {
            val freedBuffers = IntArray(processed)
            AL10.alSourceUnqueueBuffers(audioSource.sourceId, freedBuffers)

            var refilled = false
            for (bufferId in freedBuffers) {
                if (fillNextBuffer(audioSource, bufferId)) {
                    refilled = true
                }
            }
            return refilled
        }
        return false
    }

    private fun generateSourceKey(audioKey: String?, origin: Any?): String {
        return when {
            audioKey != null -> audioKey
            origin != null -> origin.toString()
            else -> "unknown_${System.currentTimeMillis()}"
        }
    }

    private fun cleanupFinishedSources() {
        val iterator = activeSources.iterator()
        while (iterator.hasNext()) {
            val (key, audioSource) = iterator.next()
            val state = AL10.alGetSourcei(audioSource.sourceId, AL10.AL_SOURCE_STATE)

            if (state == AL10.AL_STOPPED) {
                audioSource.cleanup()
                iterator.remove()
            }
        }
    }

    actual fun play(audioSignal: Signal.AudioSignal) {
        if (!isInitialized) {
            println("OpenAL AudioOutput not initialized")
            return
        }

        val rawData = audioSignal.rawData
        if (rawData == null || rawData.isEmpty()) {
            println("AudioSignal has no raw data")
            return
        }

        println("Queueing AudioSignal with ${rawData.size} bytes")

        val queuedAudio = QueuedAudio(rawData, null, audioSignal.origin)
        audioQueue.offer(queuedAudio)
    }

    fun stopAudio(audioKey: String) {
        activeSources[audioKey]?.let { audioSource ->
            audioSource.cleanup()
            activeSources.remove(audioKey)
            println("Stopped audio: $audioKey")
        }
    }

    fun stopAllAudio() {
        activeSources.values.forEach { it.cleanup() }
        activeSources.clear()
        audioQueue.clear()
        println("Stopped all audio")
    }

    fun cleanup() {
        try {
            processingJob?.cancel()
            stopAllAudio()

            if (isInitialized) {
                ALC10.alcMakeContextCurrent(0L)
                ALC10.alcDestroyContext(context)
                ALC10.alcCloseDevice(device)

                isInitialized = false
                println("OpenAL Audio Output cleaned up")
            }
        } catch (e: Exception) {
            println("Cleanup error: ${e.message}")
        }
    }
}