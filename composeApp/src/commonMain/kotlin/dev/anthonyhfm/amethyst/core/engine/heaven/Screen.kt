package dev.anthonyhfm.amethyst.core.engine.heaven

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.engine.utils.SortedList
import kotlinx.atomicfu.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Screen : AutoCloseable {
    private class Pixel(
        private val index: Byte
    ) {
        private val signals = SortedList<Int, Signal.LED>()
        private val currentColor = atomic(Color.Black)
        private val locker = Mutex()
        private val isDirty = atomic(false)

        suspend fun clear() = locker.withLock {
            signals.clear()
            signals[10000] = Signal.LED(null, x = index % 10, y = index / 10, color = Color.Black, layer = -100)
            recomputeColorLocked()
            isDirty.value = true
        }

        private fun recomputeColorLocked(): Color {
            var ret = Color.Black

            for (i in 0 until signals.size) {
                val signal = signals.getValueAt(i)

                if (signal.blendingMode != Signal.LED.BlendingMode.Normal &&
                    (i == signals.size - 1 ||
                     signal.layer - signals.getValueAt(i + 1).layer > signal.blendingRange)) {
                    continue
                }

                if (signal.blendingMode == Signal.LED.BlendingMode.Mask) break

                val multiply = i > 0 &&
                              signals.getValueAt(i - 1).blendingMode == Signal.LED.BlendingMode.Multiply &&
                              signals.getValueAt(i - 1).layer - signal.layer <= signals.getValueAt(i - 1).blendingRange

                ret = ret.mix(signal.color, multiply)

                if (signal.blendingMode == Signal.LED.BlendingMode.Normal) break
            }

            currentColor.value = ret
            return ret
        }

        fun getCachedColor(): Color = currentColor.value

        suspend fun midiEnter(n: Signal.LED) = locker.withLock {
            if (n.y * 10 + n.x != index.toInt()) return@withLock

            val layer = -n.layer

            if (n.color.isLit()) {
                signals[layer] = n.copy()
            } else if (signals.containsKey(layer)) {
                signals.remove(layer)
            }

            recomputeColorLocked()
            isDirty.value = true
        }

        fun consumeDirtyFlag(): Boolean {
            return if (isDirty.value) {
                isDirty.value = false
                true
            } else false
        }
    }

    var screenExit: ((List<RawLEDUpdate>, Array<Color>) -> Unit)? = null

    private val screen = Array(101) { Pixel(it.toByte()) }
    private val snapshot = Array(101) { Color.Black }

    private val dirtyMutex = Mutex()
    private val dirtyIndices = mutableSetOf<Int>()

    suspend fun clear() {
        screen.forEach { it.clear() }
        dirtyMutex.withLock {
            dirtyIndices.clear()
            for (i in screen.indices) dirtyIndices.add(i)
        }
        snapshot()
    }

    private suspend fun snapshot() {
        val updates = mutableListOf<RawLEDUpdate>()

        val localDirty = dirtyMutex.withLock {
            val copy = dirtyIndices.toList()
            dirtyIndices.clear()
            copy
        }

        if (localDirty.isEmpty()) {
            return
        }

        for (i in localDirty) {
            val newColor = screen[i].getCachedColor()

            if (snapshot[i] != newColor) {
                updates.add(RawLEDUpdate(i, newColor))
                snapshot[i] = newColor
            }
        }

        if (updates.isNotEmpty()) {
            screenExit?.invoke(updates, snapshot)
        }
    }

    companion object {
        private val drawingHandlers = atomic(listOf<suspend () -> Unit>())

        suspend fun draw() {
            drawingHandlers.value.forEach { it.invoke() }
        }

        internal fun addDrawingHandler(handler: suspend () -> Unit) {
            while (true) {
                val current = drawingHandlers.value
                val new = current + handler
                if (drawingHandlers.compareAndSet(current, new)) break
            }
        }

        internal fun removeDrawingHandler(handler: suspend () -> Unit) {
            while (true) {
                val current = drawingHandlers.value
                val new = current - handler
                if (drawingHandlers.compareAndSet(current, new)) break
            }
        }
    }

    private val snapshotHandler: suspend () -> Unit = { snapshot() }

    init {
        addDrawingHandler(snapshotHandler)
    }

    fun getColor(index: Int): Color = snapshot[index]

    suspend fun midiEnter(n: Signal.LED) {
        val idx = n.x + n.y * 10
        screen[idx].midiEnter(n)

        dirtyMutex.withLock { dirtyIndices.add(idx) }
    }

    override fun close() {
        removeDrawingHandler(snapshotHandler)
    }
}

fun Color.mix(other: Color, multiply: Boolean = false): Color {
    return if (multiply) {
        Color(
            (red * other.red),
            (green * other.green),
            (blue * other.blue),
            alpha
        )
    } else {
        Color(
            (red + other.red).coerceIn(0f, 1f),
            (green + other.green).coerceIn(0f, 1f),
            (blue + other.blue).coerceIn(0f, 1f),
            alpha
        )
    }
}

fun Color.isLit(): Boolean = red > 0f || green > 0f || blue > 0f

val Signal.LED.blendingRange: Int get() = 1
