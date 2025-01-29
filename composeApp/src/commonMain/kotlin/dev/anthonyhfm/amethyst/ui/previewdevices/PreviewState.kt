package dev.anthonyhfm.amethyst.ui.previewdevices

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import dev.anthonyhfm.amethyst.core.midi.data.MidiEffectData
import kotlinx.coroutines.flow.asStateFlow

class PreviewState {
    private val _grid: MutableStateFlow<List<List<MidiEffectData>>> = MutableStateFlow(
        value = List(10) { x ->
            List(10) { y ->
                MidiEffectData(
                    x = x,
                    y = y,
                    r = 0,
                    g = 0,
                    b = 0
                )
            }
        }
    )

    val grid: StateFlow<List<List<MidiEffectData>>> = _grid.asStateFlow()

    suspend fun sendToPreview(data: MidiEffectData) {
        // Optimized version to only modify the specific cell
        _grid.emit(
            _grid.value.mapIndexed { x, row ->
                if (data.x == x) {
                    row.mapIndexed { y, effectData ->
                        if (y == data.y) data else effectData
                    }
                } else {
                    row
                }
            }
        )
    }
}

@Composable
fun rememberPreviewState(): PreviewState {
    val state = remember {
        PreviewState()
    }

    return state
}