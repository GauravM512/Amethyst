package dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MidiChord
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.ableton.AbletonChordChainDeviceState

class MidiChordAdapter(
    private val device: MidiChord
) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> {
        return listOf(
            AbletonChordChainDeviceState(
                shifts = chordShifts(listOf(
                    device.shift1,
                    device.shift2,
                    device.shift3,
                    device.shift4,
                    device.shift5,
                    device.shift6
                ).map { it.manual.value })
            )
        ).withMuteState(device.on.manual.value)
    }
}

internal fun chordShifts(shifts: List<Int>): List<Int> =
    listOf(0) + shifts.filter { it != 0 }.distinct()
