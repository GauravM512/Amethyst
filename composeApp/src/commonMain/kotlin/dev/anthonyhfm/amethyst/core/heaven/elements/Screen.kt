package dev.anthonyhfm.amethyst.core.heaven.elements

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.runBlocking

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.sign

class Screen : AutoCloseable {
    private class Pixel(
        private val index: Byte
    ) {
        private val signals: MutableMap<Int, Signal> = mutableMapOf()
        private var state: Color = Color.Black
        private val locker = Mutex()

        init {
            runBlocking {
                clear()
            }
        }

        suspend fun clear() = locker.withLock {
            signals.clear()
            signals[10000] = Signal(null, null, x = index % 10, y = index / 10, color = Color.Black, layer = -100)
        }

        suspend fun getColor(): Color = locker.withLock {
            var ret = Color.Black

            ret = signals.entries.minByOrNull { it.key }!!.value.color

            return ret
        }

        suspend fun midiEnter(n: Signal) = locker.withLock {
            if (n.y * 10 + n.x != index.toInt()) return@withLock

            val layer = -n.layer

            if (n.color != Color.Black) {
                signals[layer] = n.copy()
            } else if (layer in signals) {
                signals.remove(layer)
            }
        }
    }

    var screenExit: ((List<RawUpdate>, Array<Color>) -> Unit)? = null

    private val screen = Array(101) { Pixel(index = it.toByte()) }

    private val snapshot = Array(101) { Color(0x00000000) }

    suspend fun clear() {
        screen.forEach { it.clear() }

        snapshot()
    }

    private suspend fun snapshot() {
        val updates = mutableListOf<RawUpdate>()

        for (i in screen.indices) {
            val color = screen[i].getColor()

            if (snapshot[i] != color) {
                updates.add(RawUpdate(i, color.copy()))
                snapshot[i] = color
            }
        }

        if (updates.isNotEmpty()) {
            screenExit?.invoke(updates, snapshot)
        }
    }

    companion object {
        private val drawingHandlers = mutableListOf<suspend () -> Unit>()

        suspend fun draw() {
            drawingHandlers.forEach { it.invoke() }
        }
    }

    init {
        drawingHandlers.add { snapshot() }
    }

    fun getColor(index: Int): Color = snapshot[index]

    suspend fun midiEnter(n: Signal) {
        screen[n.x + n.y * 10].midiEnter(n)
    }

    override fun close() {
        drawingHandlers.remove { snapshot() }
    }
}
