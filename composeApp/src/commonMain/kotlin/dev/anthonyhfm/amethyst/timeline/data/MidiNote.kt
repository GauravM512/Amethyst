package dev.anthonyhfm.amethyst.timeline.data

import kotlinx.serialization.Serializable

/**
 * Represents a single MIDI note with pitch, velocity, and timing information.
 * 
 * @property pitch MIDI note number (0-127), where 60 is middle C
 * @property velocity Note velocity (0-127), representing how hard the note is played
 * @property startTimeMs Start time of the note in milliseconds relative to the track
 * @property durationMs Duration of the note in milliseconds
 */
@Serializable
data class MidiNote(
    val pitch: Int,
    val velocity: Int,
    val startTimeMs: Long,
    val durationMs: Long
) {
    init {
        require(pitch in 0..127) { "MIDI pitch must be between 0 and 127, got $pitch (note: launchpad uses 0-99)" }
        require(velocity in 0..127) { "MIDI velocity must be between 0 and 127, got $velocity" }
        require(startTimeMs >= 0) { "Start time must be non-negative, got $startTimeMs" }
        require(durationMs > 0) { "Duration must be positive, got $durationMs" }
    }
    
    val endTimeMs: Long get() = startTimeMs + durationMs
}
