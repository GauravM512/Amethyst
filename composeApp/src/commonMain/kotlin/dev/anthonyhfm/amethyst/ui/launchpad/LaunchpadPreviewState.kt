package dev.anthonyhfm.amethyst.ui.launchpad

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.heaven.RawLEDUpdate
import dev.anthonyhfm.amethyst.core.util.mainDispatcherOrDefault
import kotlin.math.roundToInt
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private val MICRO_LIGHT_GRADE = intArrayOf(
    0, 17, 21, 23, 25, 26, 27, 28,
    29, 30, 30, 31, 32, 32, 33, 33,
    34, 35, 35, 36, 36, 37, 38, 38,
    39, 39, 40, 41, 41, 42, 42, 43,
    44, 44, 45, 46, 46, 47, 47, 48,
    49, 49, 50, 50, 51, 52, 52, 53,
    53, 54, 55, 55, 56, 56, 57, 58,
    58, 59, 59, 60, 61, 61, 62, 63
)

fun Color.applyMicroLightGrade(): Color {
    val r = MICRO_LIGHT_GRADE[(red * 63f).roundToInt().coerceIn(0, 63)] / 63f
    val g = MICRO_LIGHT_GRADE[(green * 63f).roundToInt().coerceIn(0, 63)] / 63f
    val b = MICRO_LIGHT_GRADE[(blue * 63f).roundToInt().coerceIn(0, 63)] / 63f
    return Color(red = r, green = g, blue = b, alpha = alpha)
}

class LaunchpadPreviewState : AutoCloseable { // TODO: Replace with Heaven's screen-class
    private val scope = CoroutineScope(
        mainDispatcherOrDefault("LaunchpadPreviewState") + SupervisorJob()
    )
    private val lock = SynchronizedObject()
    private val updatePending = atomic(false)

    private val internalGrid = Array(100) { RawLEDUpdate(it.toByte(), Color.Black) }

    val grid: MutableState<List<RawLEDUpdate>> = mutableStateOf(
        List(100) {
            RawLEDUpdate(
                index = it,
                color = Color.Black
            )
        }
    )

    fun sendToPreview(updates: List<RawLEDUpdate>) {
        synchronized(lock) {
            for (u in updates) {
                val idx = u.index.toInt()
                if (idx in 0 until 100) {
                    internalGrid[idx].color = u.color
                }
            }
        }
        scheduleFlush()
    }

    fun clear() {
        synchronized(lock) {
            for (i in 0 until 100) {
                internalGrid[i].color = Color.Black
            }
        }
        scheduleFlush()
    }

    private fun scheduleFlush() {
        if (updatePending.compareAndSet(expect = false, update = true)) {
            scope.launch {
                flush()
            }
        }
    }

    private fun flush() {
        val snapshot = synchronized(lock) {
            updatePending.value = false
            List(100) { i ->
                RawLEDUpdate(i.toByte(), internalGrid[i].color)
            }
        }
        grid.value = snapshot
    }

    override fun close() {
        scope.cancel()
    }
}

@Composable
fun rememberLaunchpadPreviewState(): LaunchpadPreviewState {
    val state = remember {
        LaunchpadPreviewState()
    }
    DisposableEffect(state) {
        onDispose {
            state.close()
        }
    }
    return state
}
