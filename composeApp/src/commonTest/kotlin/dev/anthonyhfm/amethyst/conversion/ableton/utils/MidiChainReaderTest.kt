package dev.anthonyhfm.amethyst.conversion.ableton.utils

import androidx.compose.ui.unit.IntOffset
import dev.anthonyhfm.amethyst.conversion.ableton.data.AbletonDevice
import dev.anthonyhfm.amethyst.conversion.ableton.data.DeviceChain
import dev.anthonyhfm.amethyst.conversion.ableton.data.MidiTrack
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MidiPitcher
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual
import dev.anthonyhfm.amethyst.devices.ableton.AbletonPitcherChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.offset.OffsetChainDeviceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MidiChainReaderTest {
    private fun track(devices: List<AbletonDevice>) = MidiTrack(
        id = 1,
        _name = MidiTrack.Name(MidiTrack.Name.EffectiveName("Effects")),
        deviceChain = DeviceChain(
            deviceChain = DeviceChain.DeviceChain(DeviceChain.DeviceChain.Devices(devices)),
        ),
    )

    @Test
    fun convertsDevicesInSourceOrder() {
        val source = track(listOf(
            MidiPitcher(1, pitch = MidiPitcher.Pitch(AbletonManual(2))),
            MidiPitcher(2, pitch = MidiPitcher.Pitch(AbletonManual(-3))),
        ))
        val result = MidiChainReader().readMidiChain(source)
        assertEquals(listOf(2, -3), result.devices.map {
            assertIs<AbletonPitcherChainDeviceState>(it).pitch
        })
        assertEquals(2, MidiChainReader.getChainWeight(source))
        assertEquals(2, MidiChainReader.getAllDevicesOfType<MidiPitcher>(source).size)
    }

    @Test
    fun crossLaunchpadRoutesAppendOutputOffsetAfterDevices() {
        val source = track(listOf(MidiPitcher(1, pitch = MidiPitcher.Pitch(AbletonManual(7)))))
        val result = MidiChainReader(outputOffset = IntOffset(10, 0)).readMidiChain(source)
        assertIs<AbletonPitcherChainDeviceState>(result.devices.first())
        val offset = assertIs<OffsetChainDeviceState>(result.devices.last())
        assertEquals(10, offset.offsetX)
        assertEquals(0, offset.offsetY)
    }

    @Test
    fun emptyChainProducesNoEffectsOrOffset() {
        val source = track(emptyList())
        assertEquals(0, MidiChainReader.getChainWeight(source))
        assertEquals(emptyList(), MidiChainReader().readMidiChain(source).devices)
    }
}
