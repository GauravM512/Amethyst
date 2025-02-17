package dev.anthonyhfm.amethyst.devices.effects.keyframes.ui

import androidx.compose.animation.core.keyframes
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesEffectDeviceState
import dev.anthonyhfm.amethyst.devices.effects.keyframes.data.Keyframe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.get

class KeyframeEditorViewModel(
    val data: MutableStateFlow<KeyframesEffectDeviceState>
) : ViewModel() {
    val state = MutableStateFlow(KeyframeEditorState())

    fun addKeyframe(atIndex: Int? = null) {
        viewModelScope.launch {
            val keyframes = data.value.keyframes.toMutableList()

            if (atIndex != null) {
                keyframes.add(atIndex, Keyframe())
            } else {
                keyframes.add(Keyframe())
            }

            data.update {
                it.copy(
                    keyframes = keyframes
                )
            }

            selectKeyframe(atIndex ?: data.value.keyframes.lastIndex)
        }
    }

    fun setKeyframeLight(x: Int, y: Int) {
        viewModelScope.launch {
            val keyframes = data.value.keyframes.toMutableList()
            val frame = keyframes[state.value.selectedKeyframe].frame

            keyframes[state.value.selectedKeyframe] = keyframes[state.value.selectedKeyframe].copy(
                frame = frame.mapIndexed { array_x, it ->
                    it.mapIndexed { array_y, data ->
                        if (array_x == x && array_y == y) {
                            data.copy(
                                r = (state.value.selectedColor.red * 63).toInt(),
                                g = (state.value.selectedColor.green * 63).toInt(),
                                b = (state.value.selectedColor.blue * 63).toInt(),
                            )
                        } else {
                            data
                        }
                    }
                }
            )

            data.update {
                it.copy(
                    keyframes = keyframes
                )
            }
        }
    }

    fun selectKeyframe(index: Int) {
        viewModelScope.launch {
            state.update {
                it.copy(
                    selectedKeyframe = index
                )
            }
        }
    }

    fun changeKeyframePosition(before: Int, after: Int) {
        viewModelScope.launch {
            data.update {
                it.copy(
                    keyframes = it.keyframes.toMutableList().apply {
                        add(before, removeAt(after))
                    }
                )
            }

            if (state.value.selectedKeyframe == after) {
                state.update {
                    it.copy(
                        selectedKeyframe = after
                    )
                }
            }
        }
    }

    fun changeColor(color: Color) {
        viewModelScope.launch {
            state.update {
                it.copy(
                    selectedColor = color
                )
            }
        }
    }
}

data class KeyframeEditorState(
    val selectedKeyframe: Int = 0,
    val selectedColor: Color = Color.White
)