package dev.anthonyhfm.amethyst.core.audio

import io.github.vinceglb.filekit.core.PlatformInputStream

expect object AudioPlayer {
    fun preloadAudio(inputStream: PlatformInputStream) : AudioClip

    fun playAudio(audioKey: String)
}