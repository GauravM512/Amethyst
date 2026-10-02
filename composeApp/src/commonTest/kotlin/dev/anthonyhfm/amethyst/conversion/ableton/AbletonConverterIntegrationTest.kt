package dev.anthonyhfm.amethyst.conversion.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.data.Ableton
import dev.anthonyhfm.amethyst.conversion.ableton.data.LiveSetData
import dev.anthonyhfm.amethyst.conversion.ableton.data.MasterTrack
import dev.anthonyhfm.amethyst.conversion.ableton.data.Tracks
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AbletonConverterIntegrationTest {
    private fun emptySet(version: String, tempo: Double): Ableton = Ableton(
        majorVersion = 5,
        minorVersion = version,
        creator = "test",
        revision = "1",
        liveSet = LiveSetData(
            tracks = Tracks(midiTracks = emptyList()),
            legacyMasterTrack = MasterTrack(
                deviceChain = MasterTrack.DeviceChain(
                    mixer = MasterTrack.Mixer(MasterTrack.Mixer.Tempo(AbletonManual(tempo))),
                ),
            ),
        ),
    )

    @Test
    fun convertsEmptyLiveSetWithTempoAndOneLaunchpad() {
        val result = AbletonConverter.runLiveConversion("Empty set", emptySet("11.3", 137.5), reporter = null)
        assertEquals("Empty set", result.title)
        assertEquals(137.5, result.settings.bpm)
        assertEquals(1, result.launchpadDevices.size)
        assertTrue(result.lights.devices.isEmpty())
        assertTrue(result.sampling.devices.isEmpty())
        assertEquals(AbletonConverter.LiveVersion.LIVE_11, AbletonConverter.liveVersion)
        assertNull(AbletonConverter.projectLayout)
        assertNull(AbletonConverter.launchpadLayout)
        assertTrue(AbletonConverter.audioMap.isEmpty())
    }

    @Test
    fun subsequentConversionUpdatesVersionAndTempoWithoutLeakingState() {
        AbletonConverter.runLiveConversion("First", emptySet("9.7", 90.0), reporter = null)
        val second = AbletonConverter.runLiveConversion("Second", emptySet("12.1", 128.0), reporter = null)
        assertEquals("Second", second.title)
        assertEquals(128.0, second.settings.bpm)
        assertEquals(AbletonConverter.LiveVersion.LIVE_12, AbletonConverter.liveVersion)
        assertNull(AbletonConverter.projectLayout)
    }
}
