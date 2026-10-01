package dev.anthonyhfm.amethyst.timeline

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.engine.heaven.Heaven
import dev.anthonyhfm.amethyst.timeline.data.GradientInterpolator
import dev.anthonyhfm.amethyst.timeline.data.NoteGradientStop
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal class PianoRollPadPreview(
    private val nowMs: () -> Double = { Heaven.time },
    private val frameIntervalMs: () -> Double = { 1000.0 / Heaven.fps.coerceAtLeast(1) },
    private val schedule: (Double, Any, () -> Unit) -> Unit = { delay, owner, frame ->
        Heaven.schedule(delayInMs = delay, owner = owner, job = frame)
    },
    private val cancel: (Any) -> Unit = { Heaven.cancelJobsForOwner(owner = it) },
    private val send: (Signal.LED) -> Unit = { Heaven.midiEnter(signals = listOf(it)) },
) {
    private class HeldPad(
        val signal: Signal.LED,
        val gradient: List<NoteGradientStop>?,
        val durationMs: Long,
        val startedAtMs: Double,
    )

    private val lock = SynchronizedObject()
    private val heldPads = mutableMapOf<Pair<Int, Int>, HeldPad>()

    fun press(
        key: Pair<Int, Int>,
        signal: Signal.LED,
        gradient: List<NoteGradientStop>?,
        durationMs: Long,
    ) = synchronized(lock) {
        release(key = key)
        val pad = HeldPad(
            signal = signal.copy(layer = Int.MAX_VALUE),
            gradient = gradient?.takeIf { it.size >= 2 }?.let(GradientInterpolator::normalize),
            durationMs = durationMs.coerceAtLeast(1L),
            startedAtMs = nowMs(),
        )
        heldPads[key] = pad
        render(key = key, pad = pad)
    }

    private fun render(key: Pair<Int, Int>, pad: HeldPad): Unit = synchronized(lock) {
        if (heldPads[key] !== pad) {
            return@synchronized
        }
        val color = pad.gradient?.let { gradient ->
            val fraction = ((nowMs() - pad.startedAtMs) % pad.durationMs / pad.durationMs).toFloat()
            val (red, green, blue) = GradientInterpolator.interpolate(gradient, fraction)
            Color(red = red, green = green, blue = blue)
        } ?: pad.signal.color
        send(pad.signal.copy(color = color))
        if (pad.gradient != null) {
            schedule(frameIntervalMs(), pad) { render(key = key, pad = pad) }
        }
    }

    fun release(key: Pair<Int, Int>) = synchronized(lock) {
        val pad = heldPads.remove(key) ?: return@synchronized
        cancel(pad)
        send(pad.signal.copy(color = Color.Black))
    }

    fun clear() = synchronized(lock) {
        heldPads.keys.toList().forEach { release(key = it) }
    }
}
