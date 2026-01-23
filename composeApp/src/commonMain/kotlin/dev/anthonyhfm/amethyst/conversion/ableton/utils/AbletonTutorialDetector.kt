package dev.anthonyhfm.amethyst.conversion.ableton.utils

import androidx.compose.ui.unit.IntOffset
import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.data.MidiTrack
import dev.anthonyhfm.amethyst.core.midi.data.DRUM_RACK_TO_XY
import dev.anthonyhfm.amethyst.workspace.data.AutoPlayData
import kotlin.math.roundToLong

object AbletonTutorialDetector {

    private const val TICKS_PER_BEAT = 96.0

    private const val VELOCITY_THRESHOLD = 41

    fun getAutoPlayData(layout: AbletonLayout, tracks: List<MidiTrack>): AutoPlayData {
        val tutorialTracks = detectPossibleTutorialTracks(layout, tracks)

        if (tutorialTracks.isEmpty()) {
            println("No tutorial tracks found")
            return AutoPlayData(emptyMap())
        }

        val rawActions: Map<Double, List<AutoPlayData.Action>> =
            if (layout is AbletonLayout.Single || tutorialTracks.size == 1) {
                getTutorialForTrack(tutorialTracks.first(), IntOffset.Zero)
            } else {
                val leftActions = getTutorialForTrack(tutorialTracks[0], IntOffset.Zero)
                val rightActions = getTutorialForTrack(tutorialTracks[1], IntOffset(x = 10, y = 0))

                val allTimes = (leftActions.keys + rightActions.keys).toSet().sorted()
                val combined = mutableMapOf<Double, List<AutoPlayData.Action>>()

                for (time in allTimes) {
                    val leftList = leftActions[time].orEmpty()
                    val rightList = rightActions[time].orEmpty()
                    if (leftList.isNotEmpty() || rightList.isNotEmpty()) {
                        combined[time] = leftList + rightList
                    }
                }

                combined
            }

        val deduped = rawActions.mapValues { (_, list) -> list.distinct() }

        if (deduped.isEmpty()) {
            return AutoPlayData(emptyMap())
        }

        val minTime = deduped.keys.minOrNull() ?: 0.0
        val normalized =
            if (minTime != 0.0) {
                val out = mutableMapOf<Double, List<AutoPlayData.Action>>()
                for ((time, list) in deduped) {
                    out[time - minTime] = list
                }
                out
            } else deduped

        return AutoPlayData(normalized)
    }

    fun detectPossibleTutorialTracks(
        layout: AbletonLayout,
        tracks: List<MidiTrack>
    ): List<MidiTrack> {
        val tutorialTracks = tracks
            .filter { track ->
                track.name.lowercase().contains("tutorial")
            }
            .sortedBy { track ->
                track.name.lowercase()
            }
            .take(if (layout is AbletonLayout.Single) 1 else 2)

        return tutorialTracks
    }

    fun getTutorialForTrack(track: MidiTrack, offset: IntOffset): Map<Double, List<AutoPlayData.Action>> {
        data class NoteEvent(
            val startTicks: Long,
            val endTicks: Long,
            val padIndex: Int
        )

        val notes = mutableListOf<NoteEvent>()

        val trackName = track.name

        println("Trying to get tutorial from Track \"$trackName\"")

        val clip = findTutorialClip(track) ?: run {
            println("No MidiClip with \"tutorial\" in name found in track \"$trackName\"")
            return emptyMap()
        }

        val clipStartBeats = clip.currentStart.value

        val clipEndBeats = clip.currentEnd.value

        println("Using MidiClip (start=$clipStartBeats, end=$clipEndBeats) in track \"$trackName\"")

        val bpm = AbletonConverter.bpm
        if (bpm <= 0.0) {
            println("AbletonConverter.bpm is <= 0 ($bpm). Cannot convert beats to ms.")
            return emptyMap()
        }

        val msPerBeat = 60000.0 / bpm
        val msPerTick = msPerBeat / TICKS_PER_BEAT

        val keyTracks = clip.notes.keyTracks.tracks

        keyTracks.forEach { keyTrack ->
            val pitch = keyTrack.midiKey.value

            val padIndex = DRUM_RACK_TO_XY[pitch]

            println("KeyTrack for pitch=$pitch mappedToPadIndex=$padIndex")

            keyTrack.notes.notes.forEach { note ->
                val velocity = note.velocity

                if (velocity < VELOCITY_THRESHOLD) {
                    return@forEach
                }

                val timeBeats = note.time
                val durationBeats = note.duration

                val absStartBeats = clipStartBeats + timeBeats
                val absEndBeats = absStartBeats + durationBeats

                val startTicks = (absStartBeats * TICKS_PER_BEAT).roundToLong()
                val endTicks = (absEndBeats * TICKS_PER_BEAT).roundToLong()

                if (endTicks < startTicks) return@forEach

                notes += NoteEvent(
                    startTicks = startTicks,
                    endTicks = endTicks,
                    padIndex = padIndex
                )
            }
        }

        if (notes.isEmpty()) {
            println("No qualifying MidiNoteEvent (velocity >= $VELOCITY_THRESHOLD) in tutorial clip of track \"$trackName\"")
            return emptyMap()
        }

        val result = mutableMapOf<Double, MutableList<AutoPlayData.Action>>()

        fun addAction(timeMs: Double, padIndex: Int, down: Boolean) {
            val x = offset.x + (padIndex % 10)
            val y = offset.y + (9 - padIndex / 10)

            val action = AutoPlayData.Action(
                x = x,
                y = y,
                down = down
            )

            val list = result.getOrPut(timeMs) { mutableListOf() }
            list += action
        }

        notes
            .sortedBy { it.startTicks }
            .forEach { note ->
                val startMs = note.startTicks * msPerTick
                val endMs = note.endTicks * msPerTick

                addAction(startMs, note.padIndex, down = true)

                if (note.endTicks > note.startTicks) {
                    addAction(endMs, note.padIndex, down = false)
                }
            }

        val collapsed: Map<Double, List<AutoPlayData.Action>> =
            result.mapValues { (_, list) ->
                list
                    .groupBy { it.x to it.y }
                    .values
                    .map { actionsAtPad ->
                        actionsAtPad.find { it.down } ?: actionsAtPad.first()
                    }
            }

        return collapsed
    }

    private fun findTutorialClip(track: MidiTrack): MidiTrack.TakeLanes.TakeLanes.TakeLane.ClipAutomation.Events.MidiClip? {
        val allClips = track.takeLanes?.takeLanes?.lanes?.first()?.clipAutomation?.events?.clips ?: emptyList()
        if (allClips.isEmpty()) return null

        return allClips.minByOrNull { clip ->
            clip.currentStart.value
        }
    }
}
