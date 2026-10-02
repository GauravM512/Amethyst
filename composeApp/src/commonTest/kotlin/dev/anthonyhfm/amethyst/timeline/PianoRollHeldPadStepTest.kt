package dev.anthonyhfm.amethyst.timeline

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.controls.selection.Selectable
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.controls.undo.UndoManager
import dev.anthonyhfm.amethyst.timeline.contract.TimelineClipContext
import dev.anthonyhfm.amethyst.timeline.contract.TimelineTimingContext
import dev.anthonyhfm.amethyst.timeline.data.MidiEntry
import dev.anthonyhfm.amethyst.timeline.data.MidiNote
import dev.anthonyhfm.amethyst.timeline.data.MidiTimelineTrack
import dev.anthonyhfm.amethyst.timeline.data.NoteGradientStop
import dev.anthonyhfm.amethyst.timeline.utils.GridUtils
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PianoRollHeldPadStepTest {
    private lateinit var mode: PianoRollWorkspaceMode

    @BeforeTest
    fun setUp() {
        SelectionManager.clear()
        UndoManager.clear()
        val entry = MidiEntry(startTimeMs = 0L, durationMs = 2000L)
        val track = MidiTimelineTrack().apply {
            entries[entry.startTimeMs] = entry
        }
        TimelineRepository.updateTracksSnapshot(listOf(track))
        mode = PianoRollWorkspaceMode().apply {
            bindClipContext(context = TimelineClipContext.midi(trackIndex = 0, entry = entry), entry = entry)
            timingContextProvider = {
                TimelineTimingContext(
                    bpm = 120.0,
                    gridType = GridUtils.GridType.Flexible.Medium,
                    zoomLevel = 1f,
                    playheadPositionMs = 0L,
                    isPlaying = false,
                )
            }
        }
    }

    @AfterTest
    fun tearDown() {
        mode.onDeactivate()
        TimelineRepository.updateTracksSnapshot(emptyList())
        SelectionManager.clear()
        UndoManager.clear()
    }

    @Test
    fun holdingAPadAndSteppingRightExtendsOneLightNote() {
        mode.pressedKeysState.value = mapOf((0 to 11) to true)
        mode.selectedColor = Color.Blue

        assertTrue(mode.stepHorizontalCursor(direction = 1))
        val originalId = mode.currentEntry!!.notes.single().noteId
        assertTrue(mode.stepHorizontalCursor(direction = 1))

        val note = mode.currentEntry!!.notes.single()
        assertEquals(originalId, note.noteId)
        assertEquals(0L, note.startTimeMs)
        assertEquals(1000L, note.durationMs)
        assertEquals(11, note.pitch)
        assertEquals(1f, note.led.blue)
        assertEquals(1000L, mode.selectedTimeMs)
    }

    @Test
    fun steppingLeftAcrossEmptySpaceOnlyMovesTheCursor() {
        mode.pressedKeysState.value = mapOf((0 to 45) to true)
        mode.selectedTimeMs = 1000L

        assertTrue(mode.stepHorizontalCursor(direction = -1))

        assertTrue(mode.currentEntry!!.notes.isEmpty())
        assertEquals(500L, mode.selectedTimeMs)
    }

    @Test
    fun steppingLeftShortensTheRunThenRemovesItsLastCell() {
        mode.pressedKeysState.value = mapOf((0 to 11) to true)
        repeat(times = 3) { mode.stepHorizontalCursor(direction = 1) }
        val originalId = mode.currentEntry!!.notes.single().noteId

        mode.stepHorizontalCursor(direction = -1)
        assertEquals(originalId, mode.currentEntry!!.notes.single().noteId)
        assertEquals(1000L, mode.currentEntry!!.notes.single().durationMs)
        mode.stepHorizontalCursor(direction = -1)
        assertEquals(500L, mode.currentEntry!!.notes.single().durationMs)
        mode.stepHorizontalCursor(direction = -1)
        assertTrue(mode.currentEntry!!.notes.isEmpty())
        assertEquals(0L, mode.selectedTimeMs)
    }

    @Test
    fun changingThePaintBeginsANewRun() {
        mode.pressedKeysState.value = mapOf((0 to 11) to true)
        mode.selectedColor = Color.Red
        mode.stepHorizontalCursor(direction = 1)
        mode.selectedColor = Color.Blue
        mode.stepHorizontalCursor(direction = 1)

        assertEquals(listOf(0L, 500L), mode.currentEntry!!.notes.map { it.startTimeMs })
        assertEquals(listOf(500L, 500L), mode.currentEntry!!.notes.map { it.durationMs })
    }

    @Test
    fun gapsAreNotFilledWhenBeginningAnotherRun() {
        mode.pressedKeysState.value = mapOf((0 to 11) to true)
        mode.stepHorizontalCursor(direction = 1)
        mode.selectedTimeMs = 1000L
        mode.stepHorizontalCursor(direction = 1)

        assertEquals(listOf(0L, 1000L), mode.currentEntry!!.notes.map { it.startTimeMs })
        assertEquals(listOf(500L, 500L), mode.currentEntry!!.notes.map { it.durationMs })
    }

    @Test
    fun creatingAndExtendingDifferentPadsIsOneUndoStep() {
        mode.pressedKeysState.value = mapOf((0 to 11) to true)
        mode.stepHorizontalCursor(direction = 1)
        val original = mode.currentEntry!!.notes.single()
        mode.pressedKeysState.value = mapOf((0 to 11) to true, (0 to 22) to true)
        mode.stepHorizontalCursor(direction = 1)

        assertEquals(listOf(1000L, 500L), mode.currentEntry!!.notes.map { it.durationMs })
        UndoManager.undo()
        val restored = (TimelineRepository.tracks.value.single() as MidiTimelineTrack).entries.getValue(0L)
        assertEquals(listOf(original), restored.notes)
        UndoManager.redo()
        val redone = (TimelineRepository.tracks.value.single() as MidiTimelineTrack).entries.getValue(0L)
        assertEquals(listOf(1000L, 500L), redone.notes.map { it.durationMs })
    }

    @Test
    fun deletingTheLastCellCanBeUndoneAndRedone() {
        mode.pressedKeysState.value = mapOf((0 to 11) to true)
        mode.stepHorizontalCursor(direction = 1)
        val original = mode.currentEntry!!.notes.single()
        mode.stepHorizontalCursor(direction = -1)
        assertTrue(mode.currentEntry!!.notes.isEmpty())

        UndoManager.undo()
        val restored = (TimelineRepository.tracks.value.single() as MidiTimelineTrack).entries.getValue(0L)
        assertEquals(listOf(original), restored.notes)
        UndoManager.redo()
        val redone = (TimelineRepository.tracks.value.single() as MidiTimelineTrack).entries.getValue(0L)
        assertTrue(redone.notes.isEmpty())
    }

    @Test
    fun gradientsRemainOnOneNoteWhileItsDurationChanges() {
        mode.gradientMode = true
        mode.workingGradient = listOf(
            NoteGradientStop(position = 0f, r = 1f, g = 0f, b = 0f),
            NoteGradientStop(position = 1f, r = 0f, g = 0f, b = 1f),
        )
        mode.pressedKeysState.value = mapOf((0 to 11) to true)
        mode.stepHorizontalCursor(direction = 1)
        val original = mode.currentEntry!!.notes.single()
        mode.stepHorizontalCursor(direction = 1)
        assertEquals(original.copy(durationMs = 1000L), mode.currentEntry!!.notes.single())
        mode.stepHorizontalCursor(direction = -1)
        assertEquals(original, mode.currentEntry!!.notes.single())
    }

    @Test
    fun legacyPadRunsAlsoExtendAndEraseWithUndo() {
        mode.bindLegacyEntry(entry = MidiEntry(startTimeMs = 0L, durationMs = 2000L))
        mode.pressedKeysState.value = mapOf((0 to 11) to true)
        mode.stepHorizontalCursor(direction = 1)
        val original = mode.currentEntry!!.notes.single()
        mode.stepHorizontalCursor(direction = 1)
        assertEquals(original.copy(durationMs = 1000L), mode.currentEntry!!.notes.single())
        UndoManager.undo()
        assertEquals(original, mode.currentEntry!!.notes.single())
        UndoManager.redo()
        assertEquals(original.copy(durationMs = 1000L), mode.currentEntry!!.notes.single())
        mode.stepHorizontalCursor(direction = -1)
        mode.stepHorizontalCursor(direction = -1)
        assertTrue(mode.currentEntry!!.notes.isEmpty())
    }

    @Test
    fun heldPadsTakePriorityOverSelectedNotes() {
        val selected = MidiNote.withColor(
            device = 0,
            pitch = 88,
            color = Color.Red,
            startTimeMs = 1000L,
            durationMs = 500L,
        )
        TimelineCommandSurface.createNotes(trackIndex = 0, entryStartMs = 0L, notes = listOf(selected))
        mode.syncCurrentEntry(entry = (TimelineRepository.tracks.value.single() as MidiTimelineTrack).entries[0L])
        SelectionManager.select(Selectable.PianoRollNote(trackIndex = 0, entryStartMs = 0L, note = selected))
        mode.pressedKeysState.value = mapOf((0 to 11) to true)

        assertTrue(mode.stepHorizontalCursor(direction = 1))

        assertEquals(selected, mode.currentEntry!!.notes.first { it.noteId == selected.noteId })
        assertEquals(0L, mode.currentEntry!!.notes.first { it.pitch == 11 }.startTimeMs)
    }

    @Test
    fun allHeldPadsReceiveTheGradientAndUndoTogether() {
        val gradient = listOf(
            NoteGradientStop(position = 0f, r = 1f, g = 0f, b = 0f),
            NoteGradientStop(position = 1f, r = 0f, g = 0f, b = 1f),
        )
        mode.gradientMode = true
        mode.workingGradient = gradient
        mode.pressedKeysState.value = mapOf((0 to 11) to true, (1 to 22) to true, (0 to 33) to false)

        assertTrue(mode.stepHorizontalCursor(direction = 1))

        val notes = mode.currentEntry!!.notes
        assertEquals(listOf(0 to 11, 1 to 22), notes.map { it.device to it.pitch })
        assertTrue(notes.all { it.led.gradient == gradient })
        UndoManager.undo()
        val restored = (TimelineRepository.tracks.value.single() as MidiTimelineTrack).entries.getValue(0L)
        assertTrue(restored.notes.isEmpty())
    }

    @Test
    fun retracingACellDoesNotStackDuplicateNotes() {
        mode.pressedKeysState.value = mapOf((0 to 11) to true)

        mode.stepHorizontalCursor(direction = 1)
        mode.stepHorizontalCursor(direction = -1)
        mode.stepHorizontalCursor(direction = 1)

        assertEquals(1, mode.currentEntry!!.notes.size)
        assertEquals(500L, mode.selectedTimeMs)
    }

    @Test
    fun steppingPastTheClipEdgesDoesNotCreateNotes() {
        mode.pressedKeysState.value = mapOf((0 to 11) to true)

        mode.stepHorizontalCursor(direction = -1)
        mode.selectedTimeMs = 2000L
        mode.stepHorizontalCursor(direction = 1)

        assertTrue(mode.currentEntry!!.notes.isEmpty())
    }

    @Test
    fun releasingPadsRestoresCursorOnlyNavigation() {
        mode.pressedKeysState.value = mapOf((0 to 11) to false)

        assertTrue(mode.stepHorizontalCursor(direction = 1))

        assertEquals(500L, mode.selectedTimeMs)
        assertTrue(mode.currentEntry!!.notes.isEmpty())
    }

    @Test
    fun arrowMovementUsesSmallLinesForSelectedNotesAndUndoKeepsTheOriginalPaint() {
        mode.pressedKeysState.value = mapOf((0 to 11) to true, (0 to 22) to true)
        mode.stepHorizontalCursor(direction = 1)
        val originalNotes = mode.currentEntry!!.notes
        assertTrue(originalNotes.all { it.durationMs == 500L })
        mode.pressedKeysState.value = emptyMap()
        SelectionManager.replaceSelections(
            updatedSelections = originalNotes.map { note ->
                Selectable.PianoRollNote(trackIndex = 0, entryStartMs = 0L, note = note)
            },
        )

        assertTrue(mode.stepHorizontalCursor(direction = 1))
        assertEquals(originalNotes.map { it.copy(startTimeMs = 125L) }, mode.currentEntry!!.notes)
        assertTrue(mode.stepHorizontalCursor(direction = -1))
        assertEquals(originalNotes, mode.currentEntry!!.notes)

        UndoManager.undo()
        val restored = (TimelineRepository.tracks.value.single() as MidiTimelineTrack).entries.getValue(0L)
        assertEquals(originalNotes.map { it.copy(startTimeMs = 125L) }, restored.notes)
        UndoManager.redo()
        val redone = (TimelineRepository.tracks.value.single() as MidiTimelineTrack).entries.getValue(0L)
        assertEquals(originalNotes, redone.notes)
    }
}
