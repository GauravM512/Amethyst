package dev.anthonyhfm.amethyst.core.engine.audio.source

/** Platform file I/O; callers validate the project hash and PCM format in the cache key. */
internal expect object PreparedAudioDiskCache {
    fun read(root: String, key: String, expectedBytes: Int): ByteArray?
    fun write(root: String, key: String, bytes: ByteArray)
}
