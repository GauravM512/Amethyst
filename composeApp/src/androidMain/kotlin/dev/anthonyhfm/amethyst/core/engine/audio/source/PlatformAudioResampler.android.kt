package dev.anthonyhfm.amethyst.core.engine.audio.source

internal actual fun platformResampleToPcm24(
    source: ByteArrayPcmAudioSource,
    outputRate: Int,
): ByteArrayPcmAudioSource? = null

internal actual fun useNativeRateForLongSample(sourceFrames: Long, sourceRate: Int): Boolean = false
