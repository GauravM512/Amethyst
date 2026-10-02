package dev.anthonyhfm.amethyst.timeline

import androidx.lifecycle.ViewModelStore
import dev.anthonyhfm.amethyst.core.controls.selection.Selectable
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.controls.undo.UndoManager
import dev.anthonyhfm.amethyst.timeline.data.MidiEntry
import dev.anthonyhfm.amethyst.timeline.data.MidiTimelineTrack
import dev.anthonyhfm.amethyst.timeline.utils.GridUtils
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineMidiInsertionTest {
    private val store = ViewModelStore()
    private lateinit var viewModel: TimelineViewModel
    private var previousGrid: GridUtils.GridType = GridUtils.GridType.NoGrid

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = UnconfinedTestDispatcher())
        previousGrid = WorkspaceRepository.gridType.value
        WorkspaceRepository.setGridType(type = GridUtils.GridType.NoGrid)
        TimelineRepository.updateTracksSnapshot(emptyList())
        SelectionManager.clear()
        UndoManager.clear()
        viewModel = TimelineViewModel()
        store.put(key = "timeline", viewModel = viewModel)
    }

    @AfterTest
    fun tearDown() {
        store.clear()
        TimelineRepository.updateTracksSnapshot(emptyList())
        SelectionManager.clear()
        UndoManager.clear()
        WorkspaceRepository.setGridType(type = previousGrid)
        Dispatchers.resetMain()
    }

    @Test
    fun timelineZoomCommandsUseTheSameExtendedLimits() {
        viewModel.setZoomLevel(zoom = 5f)
        viewModel.zoomBy(factor = 20f)
        assertEquals(100f, viewModel.viewport.value.zoomX)
        viewModel.setZoomLevel(zoom = 500f)
        assertEquals(100f, viewModel.viewport.value.zoomX)
        viewModel.setZoomLevel(zoom = 0.001f)
        assertEquals(0.0025f, viewModel.viewport.value.zoomX)
    }

    @Test
    fun insertingIntoARangeStopsBeforeTheNextClipAndCanBeUndone() {
        val existing = MidiEntry(startTimeMs = 2000L, durationMs = 1000L)
        val track = MidiTimelineTrack().apply {
            entries[existing.startTimeMs] = existing
        }
        TimelineRepository.updateTracksSnapshot(listOf(track))
        SelectionManager.select(Selectable.TimelineRange(trackIndex = 0, startMs = 500L, endMs = 2500L))

        assertTrue(viewModel.insertMidiClipForSelection())
        val updated = viewModel.tracks.value.single() as MidiTimelineTrack
        assertEquals(1500L, updated.entries.getValue(500L).durationMs)
        assertEquals(existing, updated.entries.getValue(2000L))

        UndoManager.undo()
        val restored = TimelineRepository.tracks.value.single() as MidiTimelineTrack
        assertEquals(listOf(existing), restored.entries.values.toList())
    }

    @Test
    fun insertingAtATimeCursorCreatesOneBar() {
        TimelineRepository.updateTracksSnapshot(listOf(MidiTimelineTrack()))
        SelectionManager.select(Selectable.TimelineTime(trackIndex = 0, timeMs = 750L))

        assertTrue(viewModel.insertMidiClipForSelection())
        val entry = (viewModel.tracks.value.single() as MidiTimelineTrack).entries.getValue(750L)
        assertEquals((60000.0 / WorkspaceRepository.bpm.value).toLong() * 4L, entry.durationMs)
        assertTrue(entry.notes.isEmpty())
    }

    @Test
    fun insertingInsideAnExistingClipDoesNotReplaceIt() {
        val existing = MidiEntry(startTimeMs = 500L, durationMs = 1000L)
        val track = MidiTimelineTrack().apply {
            entries[existing.startTimeMs] = existing
        }
        TimelineRepository.updateTracksSnapshot(listOf(track))
        SelectionManager.select(Selectable.TimelineTime(trackIndex = 0, timeMs = 750L))

        assertFalse(viewModel.insertMidiClipForSelection())
        assertEquals(listOf(existing), track.entries.values.toList())
    }

    @Test
    fun openingANewClipAndReturningToThePreviousModeDoesNotReenterDeactivation() {
        val previousMode = WorkspaceRepository.mode.value
        TimelineRepository.updateTracksSnapshot(listOf(MidiTimelineTrack()))
        try {
            viewModel.onDoubleClickMidiTrack(trackIndex = 0, timeMs = 500L)
            assertIs<PianoRollWorkspaceMode>(WorkspaceRepository.mode.value)
            assertEquals(1, (viewModel.tracks.value.single() as MidiTimelineTrack).entries.size)

            WorkspaceRepository.switchToPreviousMode()
            assertEquals(previousMode, WorkspaceRepository.mode.value)
        } finally {
            WorkspaceRepository.switchMode(mode = previousMode, undoable = false)
        }
    }
}
