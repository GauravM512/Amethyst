package dev.anthonyhfm.amethyst.devices.ableton

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AbletonNoteSpaceTest {
    @Test
    fun finalProjectionPreservesCrossLaunchpadOffsets() {
        val input = Signal.LED(origin = null, x = 11, y = 8, color = Color.White)
        val note = AbletonNoteSpace.Note(pitch = 36, targetX = 10, targetY = 0)
        val pitched = AbletonNoteSpace.withPitch(input, note, 36) as Signal.LED

        assertEquals(1, AbletonNoteSpace.project(pitched.copy(x = pitched.x - 10))?.x)
    }

    @Test
    fun pitcherLimitsTransposedNotesToTheMidiRange() {
        val input = Signal.LED(origin = null, x = 2, y = 1, color = Color.White)

        fun outputPitch(source: Int, shift: Int): Int {
            val note = AbletonNoteSpace.Note(pitch = source, targetX = 0, targetY = 0)
            val sourceSignal = AbletonNoteSpace.withPitch(input, note, source) as Signal.LED
            val output = mutableListOf<Signal>()
            val pitcher = AbletonPitcherChainDevice().apply {
                state.value = AbletonPitcherChainDeviceState(pitch = shift)
                signalExit = { output.addAll(it) }
            }

            pitcher.signalEnter(listOf(sourceSignal))
            return (output.single() as Signal.LED).extras.getValue(AbletonNoteSpace.PITCH)
        }

        assertEquals(0, outputPitch(source = 65, shift = -128))
        assertEquals(127, outputPitch(source = 36, shift = 128))
    }

    @Test
    fun hiddenPitchSurvivesMultiplePitchersUntilItReachesAPad() {
        val input = Signal.LED(
            origin = null,
            x = -1,
            y = -1,
            color = Color.Red,
        )
        val note = AbletonNoteSpace.Note(pitch = 26, targetX = 10, targetY = 0)
        val hidden = AbletonNoteSpace.withPitch(
            signal = input,
            note = note,
            pitch = 26,
        ) as Signal.LED

        assertNull(AbletonNoteSpace.project(hidden))

        val first = AbletonPitcherChainDevice().apply {
            state.value = AbletonPitcherChainDeviceState(pitch = 5)
        }
        val second = AbletonPitcherChainDevice().apply {
            state.value = AbletonPitcherChainDeviceState(pitch = 5)
        }
        val output = mutableListOf<Signal>()
        first.signalExit = second::signalEnter
        second.signalExit = { signals -> output.addAll(signals) }

        first.signalEnter(listOf(hidden))

        val transformed = output.single() as Signal.LED
        assertEquals(36, transformed.extras[AbletonNoteSpace.PITCH])
        assertEquals(11, AbletonNoteSpace.project(transformed)?.x)
        assertEquals(8, AbletonNoteSpace.project(transformed)?.y)
    }

    @Test
    fun targetsStayIndependentAndInvalidNotesDoNotAliasCornerPad() {
        val left = Signal.LED(origin = null, x = 1, y = 8, color = Color.Blue)
        val right = Signal.LED(origin = null, x = 11, y = 8, color = Color.Blue)
        val leftNote = AbletonNoteSpace.Note(pitch = 36, targetX = 0, targetY = 0)
        val rightNote = AbletonNoteSpace.Note(pitch = 36, targetX = 10, targetY = 0)

        assertEquals(1, AbletonNoteSpace.project(AbletonNoteSpace.withPitch(left, leftNote, 36) as Signal.LED)?.x)
        assertEquals(11, AbletonNoteSpace.project(AbletonNoteSpace.withPitch(right, rightNote, 36) as Signal.LED)?.x)
        assertNull(AbletonNoteSpace.padIndex(0))
        assertNull(AbletonNoteSpace.padIndex(126))
        assertNull(AbletonNoteSpace.withPitch(left, leftNote, 128))
    }
}
