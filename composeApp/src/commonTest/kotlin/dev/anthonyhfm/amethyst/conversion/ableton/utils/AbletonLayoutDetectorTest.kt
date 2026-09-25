package dev.anthonyhfm.amethyst.conversion.ableton.utils

import dev.anthonyhfm.amethyst.conversion.ableton.data.AbletonDevice
import dev.anthonyhfm.amethyst.conversion.ableton.data.DeviceChain
import dev.anthonyhfm.amethyst.conversion.ableton.data.MidiTrack
import dev.anthonyhfm.amethyst.conversion.ableton.data.OriginalSimpler
import dev.anthonyhfm.amethyst.conversion.ableton.data.TrackMixer
import dev.anthonyhfm.amethyst.conversion.ableton.data.TrackRouting
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonOn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AbletonLayoutDetectorTest {
    private fun sample(): OriginalSimpler = OriginalSimpler(
        player = OriginalSimpler.Player(
            multiSampleMap = OriginalSimpler.Player.MultiSampleMap(
                sampleParts = OriginalSimpler.Player.MultiSampleMap.SampleParts(multiSamplePart = null),
            ),
        ),
        volumeAndPan = OriginalSimpler.VolumeAndPan(),
    )

    private fun track(
        id: Int,
        name: String,
        audio: Boolean,
        routing: String = "",
        enabled: Boolean = true,
    ): MidiTrack = MidiTrack(
        id = id,
        _name = MidiTrack.Name(MidiTrack.Name.EffectiveName(name)),
        deviceChain = DeviceChain(
            midiInputRouting = TrackRouting(TrackRouting.Target(routing)),
            mixer = TrackMixer(
                on = AbletonOn(AbletonManual(enabled)),
                speaker = AbletonOn(AbletonManual(enabled)),
            ),
            deviceChain = DeviceChain.DeviceChain(
                DeviceChain.DeviceChain.Devices(if (audio) listOf<AbletonDevice>(sample()) else emptyList()),
            ),
        ),
    )

    @Test
    fun emptySetHasNoAudioOrLightTrack() {
        val layout = assertIs<AbletonLayout.Single>(AbletonLayoutDetector.detectLayout(emptyList()))
        assertNull(layout.audioTrack)
        assertNull(layout.lightsTrack)
    }

    @Test
    fun separatesSampleTracksFromLightTracks() {
        val audio = track(1, "Samples", true)
        val lights = track(2, "Lights", false)
        val layout = assertIs<AbletonLayout.Single>(AbletonLayoutDetector.detectLayout(listOf(lights, audio)))
        assertEquals(audio.id, layout.audioTrack?.id)
        assertEquals(lights.id, layout.lightsTrack?.id)
    }

    @Test
    fun detectsDualTwoLightLayoutInNameOrder() {
        val tracks = listOf(track(1, "R Audio", true), track(2, "R Light", false), track(3, "L Audio", true), track(4, "L Light", false))
        val layout = assertIs<AbletonLayout.Dual2Light>(AbletonLayoutDetector.detectLayout(tracks))
        assertEquals(3, layout.audioLeft?.id)
        assertEquals(1, layout.audioRight?.id)
        assertEquals(4, layout.lightsLeft?.id)
        assertEquals(2, layout.lightsRight?.id)
    }

    @Test
    fun detectsDualFourLightLayout() {
        val tracks = listOf(track(1, "B", true), track(2, "A", true)) +
            (1..4).map { track(it + 2, "Light $it", false) }
        val layout = assertIs<AbletonLayout.Dual4Light>(AbletonLayoutDetector.detectLayout(tracks))
        assertEquals(2, layout.audioLeft?.id)
        assertEquals(1, layout.audioRight?.id)
        assertEquals(listOf(3, 4, 5, 6), listOf(layout.lightsLeft?.id, layout.lightsLeftToRight?.id, layout.lightsRightToLeft?.id, layout.lightsRight?.id))
    }

    @Test
    fun groupsAudibleParallelSamplesByRoutingAndExcludesMutedTracks() {
        val primary = track(1, "Primary", true, "bus")
        val parallel = track(2, "Parallel", true, "bus")
        val muted = track(3, "Muted", true, "bus", enabled = false)
        val other = track(4, "Other", true, "other")
        val result = AbletonLayoutDetector.findAudibleAudioTracks(
            AbletonLayout.Single(primary, null), listOf(primary, parallel, muted, other),
        )
        assertEquals(listOf(1, 2), result.left.map(MidiTrack::id))
        assertEquals(emptyList(), result.right)
    }
}
