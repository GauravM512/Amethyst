package dev.anthonyhfm.amethyst.devices.effects.keyframes

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class KeyframesWorkspaceModeViewModel : ViewModel() {
    val state = MutableStateFlow(KeyframesUIState())
    
    fun setDrawColor(color: Color) {
        state.update { it.copy(drawColor = color) }
    }
    
    fun addColorToHistory(color: Color) {
        state.update { currentState ->
            if (!currentState.usedColors.contains(color)) {
                currentState.copy(usedColors = currentState.usedColors + color)
            } else {
                currentState
            }
        }
    }
    
    fun setCurrentFrame(frameId: Int) {
        state.update { it.copy(currentFrame = frameId) }
    }
    
    fun nextFrame() {
        state.update { it.copy(currentFrame = it.currentFrame + 1) }
    }
    
    fun previousFrame() {
        if (state.value.currentFrame > 0) {
            state.update { it.copy(currentFrame = it.currentFrame - 1) }
        }
    }
    
    fun setFrameDuration(frameId: Int, frameDuration: Double) {
        state.update { currentState ->
            val updatedDurations = currentState.frameDurations.toMutableMap().apply {
                this[frameId] = frameDuration
            }
            currentState.copy(frameDurations = updatedDurations)
        }
    }
}

data class KeyframesUIState(
    val drawColor: Color = Color.White,
    val currentFrame: Int = 0,
    val isPlaying: Boolean = false,
    val frameDurations: Map<Int, Double> = mapOf(),
    val usedColors: List<Color> = listOf(Color.White, Color.Red, Color.Green, Color.Blue)
) {
    fun getFrameDuration(frameId: Int): Double {
        return frameDurations[frameId] ?: 200.0
    }
}
