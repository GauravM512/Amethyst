package dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.DrumGroupDevice
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.audio.effects.StereoGainChainDeviceState
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AbletonMixerVolumeTest {

    @Test
    fun parsesRackBranchMixerVolumes() {
        val xml = """<MixerDevice><Speaker><Manual Value="true"/></Speaker><Volume><Manual Value="1.49623561"/></Volume></MixerDevice>"""

        val instrument = AbletonConverter.xml.decodeFromString(
            deserializer = InstrumentGroupDevice.Branches.InstrumentBranch.MixerDevice.serializer(),
            string = xml,
        )
        val drum = AbletonConverter.xml.decodeFromString(
            deserializer = DrumGroupDevice.Branches.DrumBranch.MixerDevice.serializer(),
            string = xml,
        )

        assertEquals(1.49623561f, instrument.volume.manual.value)
        assertEquals(1.49623561f, drum.volume.manual.value)
    }

    @Test
    fun branchMixerBoostBecomesStereoGain() {
        val states = mutableListOf<DeviceState>()
        states.appendMixerVolume(linearVolume = 1.49623561f)

        val gain = assertIs<StereoGainChainDeviceState>(states.single())
        assertTrue(abs(gain.gainDb - 3.5f) < 0.01f)
    }

    @Test
    fun unityBranchVolumeDoesNotAddAnAudioDevice() {
        val states = mutableListOf<DeviceState>()
        states.appendMixerVolume(linearVolume = 1f)
        assertEquals(0, states.size)
    }
}
