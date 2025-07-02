package dev.anthonyhfm.amethyst.core.audio

import io.github.vinceglb.filekit.core.PlatformInputStream

actual object AudioPlayer {
    actual fun preloadAudio(inputStream: PlatformInputStream): AudioClip {
        TODO()
    }

    actual fun playAudio(audioKey: String) {
    }
}