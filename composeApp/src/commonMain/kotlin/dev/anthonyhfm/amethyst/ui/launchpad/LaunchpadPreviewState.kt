package dev.anthonyhfm.amethyst.ui.launchpad

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.heaven.RawLEDUpdate

class LaunchpadPreviewState { // TODO: Replace with Heaven's screen-class
    val grid: MutableState<List<RawLEDUpdate>> = mutableStateOf(
        List(100) {
            RawLEDUpdate(
                index = it,
                color = Color.Black
            )
        }
    )

    fun sendToPreview(updates: List<RawLEDUpdate>) {
        grid.value = grid.value.toMutableList().apply {
            updates.forEach {
                this[it.index.toInt()] = it
            }
        }
    }

    fun clear() {
        grid.value = List(100) {
            RawLEDUpdate(
                index = it,
                color = Color.Black
            )
        }
    }
}

@Composable
fun rememberLaunchpadPreviewState(): LaunchpadPreviewState {
    val state = remember {
        LaunchpadPreviewState()
    }

    return state
}