package dev.anthonyhfm.amethyst.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import dev.anthonyhfm.amethyst.timeline.data.MidiEntry
import dev.anthonyhfm.amethyst.timeline.data.MidiNote
import dev.anthonyhfm.amethyst.timeline.ui.views.PianoRollView
import dev.anthonyhfm.amethyst.workspace.WorkspaceContract
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Piano Roll Workspace Mode for editing MIDI notes in a dedicated view.
 * Similar to KeyframesWorkspaceMode, this provides a focused editing environment.
 */
class PianoRollWorkspaceMode : WorkspaceContract.WorkspaceMode {
    override val displayName: String = "Piano Roll"
    override val selectable: Boolean = false
    override val claimInputs: Boolean = true

    // The MIDI entry being edited
    var currentEntry: MidiEntry? = null
    var trackIndex: Int = -1
    var entryStartMs: Long = 0L
    
    // Callbacks for modifications
    var onNoteAdd: ((MidiNote) -> Unit)? = null
    var onNoteUpdate: ((MidiNote, MidiNote) -> Unit)? = null
    var onNoteDelete: ((MidiNote) -> Unit)? = null
    var modeClose: (() -> Unit)? = null

    @Composable
    fun ModeContent(paddingValues: PaddingValues) {
        val entry = currentEntry ?: return

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            PianoRollView(
                entry = entry,
                widthPx = 10000f, // Large width for scrollable piano roll
                heightPx = 800f,
                zoomLevel = 0.1f, // Adjust as needed
                onNoteClick = { note ->
                    println("Piano Roll: Note clicked - pitch=${note.pitch}, vel=${note.velocity}")
                },
                onNoteMove = { note, newStartMs, newPitch ->
                    val updatedNote = note.copy(
                        startTimeMs = newStartMs,
                        pitch = newPitch
                    )
                    onNoteUpdate?.invoke(note, updatedNote)
                },
                onNoteResize = { note, newDurationMs ->
                    val updatedNote = note.copy(durationMs = newDurationMs)
                    onNoteUpdate?.invoke(note, updatedNote)
                },
                onNoteCreate = { pitch, startTimeMs, durationMs ->
                    // Limit pitch to 0-99 for launchpad compatibility
                    val limitedPitch = pitch.coerceIn(0, 99)
                    val newNote = MidiNote(
                        pitch = limitedPitch,
                        velocity = 100, // Default velocity
                        startTimeMs = startTimeMs,
                        durationMs = durationMs
                    )
                    onNoteAdd?.invoke(newNote)
                },
                onNoteDelete = { note ->
                    onNoteDelete?.invoke(note)
                }
            )
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.type == KeyEventType.KeyDown) {
            when (event.key) {
                Key.Escape -> {
                    modeClose?.invoke()
                    return true
                }
                Key.Z -> {
                    if (event.isCtrlPressed || event.isMetaPressed) {
                        // Handle undo if needed
                        return true
                    }
                }
            }
        }
        return false
    }
}
