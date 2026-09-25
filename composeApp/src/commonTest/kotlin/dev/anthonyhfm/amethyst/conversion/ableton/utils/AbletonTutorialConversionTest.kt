package dev.anthonyhfm.amethyst.conversion.ableton.utils

import androidx.compose.ui.unit.IntOffset
import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.data.DeviceChain
import dev.anthonyhfm.amethyst.conversion.ableton.data.MidiClip
import dev.anthonyhfm.amethyst.conversion.ableton.data.MidiTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AbletonTutorialConversionTest {
    private fun clip(pitch: Int = 36, velocity: Float = 100f): MidiClip = MidiClip(
        time = 0.0,
        currentStart = MidiClip.CurrentTimeStamp(4.0),
        currentEnd = MidiClip.CurrentTimeStamp(8.0),
        notes = MidiClip.Notes(MidiClip.Notes.KeyTracks(listOf(
            MidiClip.Notes.KeyTracks.KeyTrack(
                notes = MidiClip.Notes.KeyTracks.KeyTrack.Notes(listOf(
                    MidiClip.Notes.KeyTracks.KeyTrack.Notes.MidiNoteEvent(1.0, 0.5, velocity),
                )),
                midiKey = MidiClip.Notes.KeyTracks.KeyTrack.MidiKey(pitch),
            ),
        ))),
    )

    private fun tutorialTrack(name: String = "Tutorial", clip: MidiClip = clip()): MidiTrack = MidiTrack(
        id = 1,
        _name = MidiTrack.Name(MidiTrack.Name.EffectiveName(name)),
        deviceChain = DeviceChain(
            deviceChain = DeviceChain.DeviceChain(DeviceChain.DeviceChain.Devices(emptyList())),
            mainSequencer = DeviceChain.MainSequencer(DeviceChain.MainSequencer.ClipTimeable(
                DeviceChain.MainSequencer.ClipTimeable.ArrangerAutomation(
                    DeviceChain.MainSequencer.ClipTimeable.ArrangerAutomation.Events(listOf(clip)),
                ),
            )),
        ),
    )

    @Test
    fun noteEventsBecomeTimedPadPressAndRelease() {
        AbletonConverter.bpm = 120.0
        val actions = AbletonTutorialDetector.getTutorialForTrack(tutorialTrack(), IntOffset(10, 0))
        val press = actions[500.0]!!.single()
        val release = actions[750.0]!!.single()
        assertTrue(press.down)
        assertFalse(release.down)
        assertEquals(press.x, release.x)
        assertEquals(press.y, release.y)
        assertTrue(press.x >= 10)
    }

    @Test
    fun zeroVelocityNotesProduceNoActions() {
        AbletonConverter.bpm = 120.0
        assertTrue(AbletonTutorialDetector.getTutorialForTrack(
            tutorialTrack(clip = clip(velocity = 0f)), IntOffset.Zero,
        ).isEmpty())
    }

    @Test
    fun unmappedMidiNotesProduceNoPadActions() {
        AbletonConverter.bpm = 120.0
        assertTrue(AbletonTutorialDetector.getTutorialForTrack(
            tutorialTrack(clip = clip(pitch = 127)), IntOffset.Zero,
        ).isEmpty())
        assertTrue(AbletonTutorialDetector.getTutorialForTrack(
            tutorialTrack(clip = clip(pitch = 200)), IntOffset.Zero,
        ).isEmpty())
    }

    @Test
    fun tutorialTrackSelectionHonorsLayoutCapacity() {
        val tracks = listOf(tutorialTrack("Tutorial B"), tutorialTrack("Tutorial A"))
        assertEquals(listOf("Tutorial A"), AbletonTutorialDetector.detectPossibleTutorialTracks(
            AbletonLayout.Single(null, null), tracks,
        ).map(MidiTrack::name))
        assertEquals(2, AbletonTutorialDetector.detectPossibleTutorialTracks(
            AbletonLayout.Dual2Light(null, null, null, null), tracks,
        ).size)
    }
}
