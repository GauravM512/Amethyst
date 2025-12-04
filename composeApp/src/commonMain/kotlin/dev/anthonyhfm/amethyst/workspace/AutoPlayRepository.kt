package dev.anthonyhfm.amethyst.workspace

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.engine.heaven.Heaven
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AutoPlayState {
    STOPPED,
    PLAYING,
    PAUSED
}

object AutoPlayRepository {
    private val _state = MutableStateFlow(AutoPlayState.STOPPED)
    val state: StateFlow<AutoPlayState> = _state.asStateFlow()

    private var pausedAtTime: Double = 0.0
    private var startOffset: Double = 0.0

    fun startAutoPlay() {
        if (_state.value == AutoPlayState.PLAYING) return
        
        val autoplay = WorkspaceRepository.saveableWorkspaceData?.autoPlay ?: return
        val settings = WorkspaceRepository.saveableWorkspaceData?.settings

        // Cancel any existing jobs
        Heaven.cancelJobsForOwner(this)

        // Calculate offset if resuming from pause
        if (_state.value == AutoPlayState.PAUSED) {
            startOffset = pausedAtTime
        } else {
            startOffset = 0.0
        }

        _state.value = AutoPlayState.PLAYING

        autoplay.actions.entries.forEach { entry ->
            val adjustedDelay = entry.key - startOffset
            if (adjustedDelay >= 0) {
                Heaven.schedule(adjustedDelay, this) {
                    // Send MIDI signals to sampling chain
                    WorkspaceRepository.samplingChain.signalEnter(
                        entry.value.map {
                            Signal.Midi(
                                origin = this,
                                x = it.x,
                                y = it.y,
                                velocity = if (it.down) 127 else 0,
                            )
                        }
                    )

                    // Send LED signals to lights chain if enabled
                    if (settings?.autoPlayShowLights == true) {
                        WorkspaceRepository.lightsChain.signalEnter(
                            entry.value.map {
                                Signal.LED(
                                    origin = this,
                                    x = it.x,
                                    y = it.y,
                                    color = if (it.down) Color.White else Color.Black,
                                )
                            }
                        )
                    }

                    // Show button presses on layer 100 if enabled
                    if (settings?.autoPlayShowButtonPresses == true) {
                        Heaven.midiEnter(
                            entry.value.map {
                                Signal.LED(
                                    origin = this,
                                    x = it.x,
                                    y = it.y,
                                    color = if (it.down) Color.White else Color.Black,
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    fun pauseAutoPlay() {
        if (_state.value != AutoPlayState.PLAYING) return
        
        pausedAtTime = Heaven.time + startOffset
        Heaven.cancelJobsForOwner(this)
        _state.value = AutoPlayState.PAUSED
    }

    fun stopAutoPlay() {
        Heaven.cancelJobsForOwner(this)
        _state.value = AutoPlayState.STOPPED
        pausedAtTime = 0.0
        startOffset = 0.0
    }

    fun resumeAutoPlay() {
        if (_state.value == AutoPlayState.PAUSED) {
            startAutoPlay()
        }
    }
}