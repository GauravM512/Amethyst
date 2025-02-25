package dev.anthonyhfm.amethyst.core.midi.devices

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.heaven.elements.RawUpdate
import dev.anthonyhfm.amethyst.core.heaven.elements.Screen
import dev.atsushieno.ktmidi.MidiOutput

abstract class LaunchpadDevice {
    val screen: Screen = Screen()

    init {
        screen.screenExit = { updates, colors ->
            sendUpdate(updates, colors)
        }
    }

    abstract var position: Pair<Int, Int>

    abstract var midiOutput: MidiOutput

    abstract fun clear()

    abstract fun sendUpdate(updates: List<RawUpdate>, colors: Array<Color>)

    abstract fun getEffectSysEx(update: RawUpdate): ByteArray
}
