package dev.anthonyhfm.amethyst.core.midi.devices

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.heaven.elements.RawUpdate
import dev.atsushieno.ktmidi.MidiOutput
import kotlinx.coroutines.launch

class LaunchpadDeviceMK2(
    override var midiOutput: MidiOutput,
) : LaunchpadDevice() {
    override fun clear() {
        val clearSysEx = byteArrayOf(240.toByte(), 0.toByte(), 32.toByte(), 41.toByte(), 2.toByte(), 24.toByte(), 14.toByte(), 0.toByte(), 247.toByte())

        sendMidi(clearSysEx)
    }

    override fun sendUpdate(updates: List<RawUpdate>, colors: Array<Color>) {
        updates.forEach {
            sendMidi(getEffectSysEx(it))
        }
    }

    override fun getEffectSysEx(update: RawUpdate): ByteArray {
        return byteArrayOf(
            240.toByte(),
            0.toByte(),
            32.toByte(),
            41.toByte(),
            2.toByte(),
            24.toByte(),
            11.toByte(),
            update.index,
            (update.color.red * 63).toInt().toByte(),
            (update.color.green * 63).toInt().toByte(),
            (update.color.blue * 63).toInt().toByte(),
            247.toByte()
        )
    }

    private fun sendMidi(data: ByteArray) {
        outscope.launch {
            midiOutput.send(data, 0, data.size, 0)
        }
    }

    companion object {
        @OptIn(ExperimentalUnsignedTypes::class)
        fun identify(inquiry: UByteArray): Boolean {
            if (inquiry.size > 18) return false

            try {
                val cutdown = inquiry.copyOfRange(2, inquiry.lastIndex - 4)

                return cutdown.contentEquals(ubyteArrayOf(0u, 6u, 2u, 0u, 32u, 41u, 105u, 0u, 0u, 0u))
            } catch (e: Exception) {
                return false
            }
        }
    }
}
