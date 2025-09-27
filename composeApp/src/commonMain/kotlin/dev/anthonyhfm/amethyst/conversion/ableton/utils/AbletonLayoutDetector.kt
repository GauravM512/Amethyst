package dev.anthonyhfm.amethyst.conversion.ableton.utils

import dev.anthonyhfm.amethyst.conversion.ableton.reader.MidiChainReader

object AbletonLayoutDetector {
    fun detectLayout(tracks: List<XmlElement>): AbletonLayout {
        val reader = MidiChainReader()

        val audioTracks: List<Pair<Int, XmlElement>> = tracks.filterNot {
            (it.querySelector("OriginalSimpler").firstOrNull() == null && it.querySelector("InstrumentGroupDevice").firstOrNull() == null)
        }.map {
            reader.getChainWeight(it) to it
        }.sortedByDescending {
            it.first
        }

        val lightsTracks: List<Pair<Int, XmlElement>> = tracks.filter {
            (it.querySelector("OriginalSimpler").firstOrNull() == null && it.querySelector("InstrumentGroupDevice").firstOrNull() == null)
        }.map {
            reader.getChainWeight(it) to it
        }.sortedByDescending {
            it.first
        }

        val maxAudio = audioTracks.firstOrNull()?.first ?: 0
        val audioCandidates = audioTracks.filter { it.first >= maxAudio * 0.3 }
            .sortedBy {
                it.second.querySelector("EffectiveName")
                    .firstOrNull()
                    ?.attributes
                    ?.get("Value")
                    ?.lowercase()
                    ?: ""
            }

        val maxLight = lightsTracks.firstOrNull()?.first ?: 0
        val lightCandidates = lightsTracks.filter { it.first >= maxLight * 0.3 }
            .sortedBy {
                it.second.querySelector("EffectiveName")
                    .firstOrNull()
                    ?.attributes
                    ?.get("Value")
                    ?.lowercase()
                    ?: ""
            }

        if (audioCandidates.size == 1 && lightCandidates.size == 1) {
            return AbletonLayout.Single(
                audioTrack = audioCandidates.first().second,
                lightsTrack = lightCandidates.first().second
            )
        } else if (audioCandidates.size == 2 && lightCandidates.size == 2) {
            return AbletonLayout.Dual2Light(
                audioLeft = audioCandidates[0].second,
                audioRight = audioCandidates[1].second,
                lightsLeft = lightCandidates[0].second,
                lightsRight = lightCandidates[1].second
            )
        } else if (audioCandidates.size == 2 && lightCandidates.size == 2) {
            return AbletonLayout.Dual4Light(
                audioLeft = audioCandidates[0].second,
                audioRight = audioCandidates[1].second,
                lightsLeft = lightCandidates[0].second,
                lightsLeftToRight = lightCandidates[1].second,
                lightsRightToLeft = lightCandidates[2].second,
                lightsRight = lightCandidates[3].second
            )
        } else {
            println("LAYOUT DETECTION ERROR: Falling back to best effort layout detection")

            return AbletonLayout.Single(
                audioTrack = audioTracks.firstOrNull()?.second,
                lightsTrack = lightsTracks.firstOrNull()?.second
            )
        }
    }
}

sealed interface AbletonLayout {
    data class Single(
        val audioTrack: XmlElement?,
        val lightsTrack: XmlElement?
    ) : AbletonLayout

    data class Dual2Light(
        val audioLeft: XmlElement?,
        val audioRight: XmlElement?,
        val lightsLeft: XmlElement?,
        val lightsRight: XmlElement?
    ) : AbletonLayout

    data class Dual4Light(
        val audioLeft: XmlElement?,
        val audioRight: XmlElement?,
        val lightsLeft: XmlElement?,
        val lightsLeftToRight: XmlElement?,
        val lightsRightToLeft: XmlElement?,
        val lightsRight: XmlElement?,
    ) : AbletonLayout
}

enum class LaunchpadSetup(val displayName: String) {
    SINGLE("Single"),
    DUAL_2_LIGHT("Dual (2 Light-Tracks)"),
    DUAL_4_LIGHT("Dual (4 Light-Tracks)")
}