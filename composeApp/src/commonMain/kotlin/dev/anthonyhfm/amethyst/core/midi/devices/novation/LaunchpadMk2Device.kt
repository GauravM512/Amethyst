package dev.anthonyhfm.amethyst.core.midi.devices.novation

import dev.anthonyhfm.amethyst.core.midi.data.MidiEffectData
import dev.anthonyhfm.amethyst.core.midi.devices.DeviceType

class LaunchpadMk2Device : DeviceType {
    override val name: String = "Launchpad Mk2"

    override fun getEffectSysEx(effect: MidiEffectData): ByteArray {
        return byteArrayOf(
            240.toByte(),
            0.toByte(),
            32.toByte(),
            41.toByte(),
            2.toByte(),
            24.toByte(),
            11.toByte(),
            (effect.y * 10 + effect.x).toByte(),
            (effect.r * 2).toByte(),
            (effect.g * 2).toByte(),
            (effect.b * 2).toByte(),
            247.toByte()
        )
    }
}