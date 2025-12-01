package dev.anthonyhfm.amethyst.conversion.ableton.utils

import androidx.compose.ui.unit.IntOffset
import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.core.midi.data.DRUM_RACK_TO_XY
import dev.anthonyhfm.amethyst.workspace.data.AutoPlayData

object AbletonTutorialDetector {

    fun getAutoPlayData(layout: AbletonLayout, tracks: List<XmlElement>): AutoPlayData {
        val tutorialTracks = detectPossibleTutorialTracks(layout, tracks)

        if (tutorialTracks.isEmpty()) {
            println("No tutorial tracks found")
            return AutoPlayData(emptyMap())
        }

        var actions: Map<Double, List<AutoPlayData.Action>> = if (layout is AbletonLayout.Single || tutorialTracks.size == 1) {
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

        actions = actions.mapValues { (_, list) -> list.distinct() }

        return AutoPlayData(actions)
    }

    /**
     * Findet potenzielle Tutorial-Tracks anhand des Track-Namens (EffectiveName enthält "tutorial").
     */
    fun detectPossibleTutorialTracks(
        layout: AbletonLayout,
        tracks: List<XmlElement>
    ): List<XmlElement> {
        val tutorialTracks = tracks
            .filter { track ->
                val name = track.querySelector("EffectiveName")
                    .firstOrNull()
                    ?.attributes
                    ?.get("Value")
                    ?.lowercase()
                    ?: ""
                name.contains("tutorial")
            }
            .sortedBy { track ->
                track.querySelector("EffectiveName")
                    .firstOrNull()
                    ?.attributes
                    ?.get("Value")
                    ?.lowercase()
                    ?: ""
            }
            .take(if (layout is AbletonLayout.Single) 1 else 2)

        return tutorialTracks
    }

    fun getTutorialForTrack(track: XmlElement, offset: IntOffset): Map<Double, List<AutoPlayData.Action>> {
        val rawActions = mutableMapOf<Double, MutableList<AutoPlayData.Action>>()

        val trackName = track.querySelector("EffectiveName")
            .firstOrNull()
            ?.attributes
            ?.get("Value")
            ?: "<unnamed>"

        println("Trying to get tutorial from Track \"$trackName\"")

        val clip = findTutorialClip(track) ?: run {
            println("No MidiClip with \"tutorial\" in name found in track \"$trackName\"")
            return emptyMap()
        }

        val clipName = clip.querySelector("Name")
            .firstOrNull()
            ?.attributes
            ?.get("Value")
            ?: "<unnamed>"

        val clipStartBeats = clip.querySelector("CurrentStart")
            .firstOrNull()
            ?.attributes
            ?.get("Value")
            ?.toDoubleOrNull()
            ?: 0.0

        val clipEndBeats = clip.querySelector("CurrentEnd")
            .firstOrNull()
            ?.attributes
            ?.get("Value")
            ?.toDoubleOrNull()
            ?: clipStartBeats

        println("Using MidiClip \"$clipName\" (start=$clipStartBeats, end=$clipEndBeats) in track \"$trackName\"")

        val bpm = AbletonConverter.bpm
        if (bpm <= 0.0) {
            println("AbletonConverter.bpm is <= 0 ($bpm). Cannot convert beats to ms.")
            return emptyMap()
        }
        val msPerBeat = 60000.0 / bpm

        val keyTracks = clip.querySelector("KeyTrack")

        keyTracks.forEach { keyTrack ->
            val pitch = keyTrack.querySelector("MidiKey")
                .firstOrNull()
                ?.attributes
                ?.get("Value")
                ?.toIntOrNull()
                ?: return@forEach

            val padIndex = DRUM_RACK_TO_XY[pitch] ?: run {
                // Pitch nicht gemappt → kein Drum Rack Pad, überspringen
                return@forEach
            }

            println("KeyTrack for pitch=$pitch mappedToPadIndex=$padIndex")

            keyTrack.querySelector("MidiNoteEvent").forEach { note ->
                // Deaktivierte Noten ignorieren
                if (note.attributes["IsEnabled"] == "false") return@forEach

                val timeBeats = note.attributes["Time"]?.toDoubleOrNull() ?: return@forEach
                val velocity = note.attributes["Velocity"]?.toIntOrNull() ?: return@forEach

                // In der Ableton-XML ist Time relativ zum Clip-Start → absolut in Beats:
                val noteAbsoluteBeats = clipStartBeats + timeBeats
                val timeMs = noteAbsoluteBeats * msPerBeat

                val x = offset.x + (padIndex % 10)
                val y = offset.y + (9 - padIndex / 10)

                val list = rawActions.getOrPut(timeMs) { mutableListOf() }
                list += AutoPlayData.Action(
                    x = x,
                    y = y,
                    down = velocity > 0
                )
            }
        }

        if (rawActions.isEmpty()) {
            println("No MidiNoteEvent found in tutorial clip \"$clipName\" of track \"$trackName\"")
            return emptyMap()
        }

        // Zeiten normalisieren: früheste Zeit = 0.0
        val firstTime = rawActions.keys.minOrNull() ?: 0.0
        val normalized = rawActions.entries.associate { (time, list) ->
            (time - firstTime) to list.toList()
        }

        return normalized
    }

    /**
     * Sucht den passenden Tutorial-Clip:
     * - erst alle Clips, deren Name "tutorial" enthält
     * - wenn keiner so heißt, nimm den frühesten Clip im Track
     */
    private fun findTutorialClip(track: XmlElement): XmlElement? {
        val allClips = track.querySelector("MidiClip")
        if (allClips.isEmpty()) return null

        val tutorialClips = allClips.filter { clip ->
            val clipName = clip.querySelector("Name")
                .firstOrNull()
                ?.attributes
                ?.get("Value")
                ?.lowercase()
                ?: ""
            clipName.contains("tutorial")
        }

        val pool = if (tutorialClips.isNotEmpty()) tutorialClips else allClips

        return pool.minByOrNull { clip ->
            clip.querySelector("CurrentStart")
                .firstOrNull()
                ?.attributes
                ?.get("Value")
                ?.toDoubleOrNull() ?: 0.0
        }
    }
}
