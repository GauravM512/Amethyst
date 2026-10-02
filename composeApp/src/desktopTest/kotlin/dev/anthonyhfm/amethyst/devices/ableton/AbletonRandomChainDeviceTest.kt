package dev.anthonyhfm.amethyst.devices.ableton

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbletonRandomChainDeviceTest {
    @Test
    fun overlappingNotesReleaseTheirOwnChosenPitches() {
        val device = AbletonRandomChainDevice().apply {
            state.value = AbletonRandomChainDeviceState(
                chance = 1.0,
                choices = 2,
                scale = 1,
                alternate = true,
            )
        }
        val output = mutableListOf<Signal.LED>()
        device.signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.LED>()) }
        val note = AbletonNoteSpace.Note(pitch = 40, targetX = 0, targetY = 0)
        val on = AbletonNoteSpace.withPitch(
            signal = Signal.LED(origin = null, x = 1, y = 2, color = Color.White),
            note = note,
            pitch = note.pitch,
        ) as Signal.LED
        val off = on.copy(color = Color.Black)

        device.signalEnter(n = listOf(on))
        device.signalEnter(n = listOf(on))
        device.signalEnter(n = listOf(off))
        device.signalEnter(n = listOf(off))

        assertEquals(listOf(40, 41, 40, 41), output.map { it.extras[AbletonNoteSpace.PITCH] })
        assertEquals(listOf(true, true, false, false), output.map { it.color != Color.Black })

        val activePitches = mutableSetOf<Int>()
        output.forEach { signal ->
            val pitch = signal.extras.getValue(AbletonNoteSpace.PITCH)
            if (signal.color == Color.Black) {
                activePitches.remove(pitch)
            } else {
                activePitches.add(pitch)
            }
        }
        assertTrue(activePitches.isEmpty())
    }
}
