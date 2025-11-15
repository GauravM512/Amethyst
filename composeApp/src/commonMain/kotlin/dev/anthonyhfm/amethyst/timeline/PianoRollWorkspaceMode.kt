package dev.anthonyhfm.amethyst.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.engine.heaven.Heaven
import dev.anthonyhfm.amethyst.timeline.data.MidiEntry
import dev.anthonyhfm.amethyst.timeline.data.MidiNote
import dev.anthonyhfm.amethyst.timeline.ui.views.PianoRollView
import dev.anthonyhfm.amethyst.workspace.WorkspaceContract
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository

/**
 * Piano Roll Workspace Mode for editing MIDI notes in a dedicated view.
 * Similar to KeyframesWorkspaceMode, this provides a focused editing environment.
 * 
 * Supports multiple launchpads - each launchpad gets 100 notes (pitch 0-99).
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
        val launchpads = Heaven.devices

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (launchpads.isEmpty()) {
                // Show message if no launchpads
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No launchpad devices found. Add a launchpad to the workspace first.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                // Show piano roll with launchpad sections
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    launchpads.forEachIndexed { index, launchpad ->
                        LaunchpadPianoRollSection(
                            entry = entry,
                            launchpadIndex = index,
                            launchpadName = launchpad.name,
                            onNoteAdd = onNoteAdd,
                            onNoteUpdate = onNoteUpdate,
                            onNoteDelete = onNoteDelete
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LaunchpadPianoRollSection(
        entry: MidiEntry,
        launchpadIndex: Int,
        launchpadName: String,
        onNoteAdd: ((MidiNote) -> Unit)?,
        onNoteUpdate: ((MidiNote, MidiNote) -> Unit)?,
        onNoteDelete: ((MidiNote) -> Unit)?
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Header for this launchpad
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Launchpad ${launchpadIndex + 1}: $launchpadName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Pitch Range: ${launchpadIndex * 100}-${launchpadIndex * 100 + 99}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Piano roll for this launchpad
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                PianoRollView(
                    entry = entry,
                    widthPx = 10000f, // Large width for scrollable content
                    heightPx = 600f,
                    zoomLevel = 0.1f,
                    minPitch = launchpadIndex * 100,
                    maxPitch = launchpadIndex * 100 + 99,
                    onNoteClick = { note ->
                        println("Piano Roll: Note clicked - pitch=${note.pitch}, vel=${note.velocity}")
                    },
                    onNoteMove = { note, newStartMs, newPitch ->
                        // Keep pitch within this launchpad's range
                        val clampedPitch = newPitch.coerceIn(launchpadIndex * 100, launchpadIndex * 100 + 99)
                        val updatedNote = note.copy(
                            startTimeMs = newStartMs,
                            pitch = clampedPitch
                        )
                        onNoteUpdate?.invoke(note, updatedNote)
                    },
                    onNoteResize = { note, newDurationMs ->
                        val updatedNote = note.copy(durationMs = newDurationMs)
                        onNoteUpdate?.invoke(note, updatedNote)
                    },
                    onNoteCreate = { pitch, startTimeMs, durationMs ->
                        // Map pitch to this launchpad's range
                        val actualPitch = launchpadIndex * 100 + pitch.coerceIn(0, 99)
                        val newNote = MidiNote(
                            pitch = actualPitch,
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
