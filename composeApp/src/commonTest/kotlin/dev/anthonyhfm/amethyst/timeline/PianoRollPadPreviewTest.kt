package dev.anthonyhfm.amethyst.timeline

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.engine.heaven.Screen
import dev.anthonyhfm.amethyst.timeline.data.NoteGradientStop
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PianoRollPadPreviewTest {
    private class PreviewHarness {
        var timeMs = 0.0
        val signals = mutableListOf<Signal.LED>()
        val pendingFrames = mutableListOf<Pair<Any, () -> Unit>>()
        val preview = PianoRollPadPreview(
            nowMs = { timeMs },
            frameIntervalMs = { 10.0 },
            schedule = { _, owner, frame -> pendingFrames.add(owner to frame) },
            cancel = { owner -> pendingFrames.removeAll { it.first === owner } },
            send = { signals.add(it) },
        )

        fun advanceTo(timeMs: Double) {
            this.timeMs = timeMs
            val frames = pendingFrames.toList()
            pendingFrames.clear()
            frames.forEach { it.second() }
        }
    }

    private val gradient = listOf(
        NoteGradientStop(position = 0f, r = 1f, g = 0f, b = 0f),
        NoteGradientStop(position = 1f, r = 0f, g = 0f, b = 1f),
    )

    private fun signal(x: Int = 11) = Signal.LED(
        origin = this,
        x = x,
        y = 8,
        color = Color.Green,
    )

    @Test
    fun solidPaintAppearsImmediatelyAndIsRemovedOnRelease() {
        val harness = PreviewHarness()
        harness.preview.press(key = 0 to 11, signal = signal(), gradient = null, durationMs = 500L)
        assertEquals(Color.Green, harness.signals.single().color)
        assertTrue(harness.pendingFrames.isEmpty())
        harness.preview.release(key = 0 to 11)
        assertEquals(Color.Black, harness.signals.last().color)
        assertEquals(harness.signals.first().layer, harness.signals.last().layer)
    }

    @Test
    fun releasingPreviewRestoresTheUnderlyingPlaybackColor() = runTest {
        val harness = PreviewHarness()
        val screen = Screen()
        try {
            screen.midiEnter(n = signal(x = 3).copy(color = Color.Blue, layer = 10))
            harness.preview.press(key = 0 to 83, signal = signal(x = 3), gradient = null, durationMs = 500L)
            screen.midiEnter(n = harness.signals.last())
            Screen.draw()
            assertEquals(Color.Green, screen.getColor(index = 83))
            harness.preview.release(key = 0 to 83)
            screen.midiEnter(n = harness.signals.last())
            Screen.draw()
            assertEquals(Color.Blue, screen.getColor(index = 83))
        } finally {
            screen.close()
        }
    }

    @Test
    fun heldGradientRepeatsAtItsDuration() {
        val harness = PreviewHarness()
        harness.preview.press(key = 0 to 11, signal = signal(), gradient = gradient, durationMs = 200L)
        assertEquals(Color.Red, harness.signals.last().color)
        harness.advanceTo(timeMs = 100.0)
        val halfway = harness.signals.last().color
        assertEquals(0.5f, halfway.red, absoluteTolerance = 1f / 255f)
        assertEquals(0.5f, halfway.blue, absoluteTolerance = 1f / 255f)
        harness.advanceTo(timeMs = 200.0)
        assertEquals(Color.Red, harness.signals.last().color)
        harness.advanceTo(timeMs = 300.0)
        assertEquals(halfway, harness.signals.last().color)
    }

    @Test
    fun releaseRejectsAlreadyDispatchedFrames() {
        val harness = PreviewHarness()
        harness.preview.press(key = 0 to 11, signal = signal(), gradient = gradient, durationMs = 200L)
        val staleFrame = harness.pendingFrames.single().second
        harness.preview.release(key = 0 to 11)
        staleFrame()
        assertEquals(Color.Black, harness.signals.last().color)
        assertTrue(harness.pendingFrames.isEmpty())
    }

    @Test
    fun repressRejectsFramesFromThePreviousPress() {
        val harness = PreviewHarness()
        harness.preview.press(key = 0 to 11, signal = signal(), gradient = gradient, durationMs = 200L)
        val staleFrame = harness.pendingFrames.single().second
        harness.preview.press(key = 0 to 11, signal = signal(), gradient = null, durationMs = 200L)
        staleFrame()
        assertEquals(Color.Green, harness.signals.last().color)
        assertTrue(harness.pendingFrames.isEmpty())
    }

    @Test
    fun simultaneousDevicesReleaseIndependentlyAndClearCancelsAllFrames() {
        val harness = PreviewHarness()
        harness.preview.press(key = 0 to 11, signal = signal(x = 11), gradient = gradient, durationMs = 200L)
        harness.preview.press(key = 1 to 11, signal = signal(x = 21), gradient = gradient, durationMs = 400L)
        harness.preview.release(key = 0 to 11)
        harness.advanceTo(timeMs = 200.0)
        assertEquals(21, harness.signals.last().x)
        assertEquals(0.5f, harness.signals.last().color.red, absoluteTolerance = 1f / 255f)
        harness.preview.clear()
        assertEquals(Color.Black, harness.signals.last().color)
        assertTrue(harness.pendingFrames.isEmpty())
    }
}
