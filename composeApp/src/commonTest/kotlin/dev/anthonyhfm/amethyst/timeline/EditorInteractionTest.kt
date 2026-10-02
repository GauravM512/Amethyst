package dev.anthonyhfm.amethyst.timeline

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.timeline.contract.GridResolution
import dev.anthonyhfm.amethyst.timeline.data.MidiNote
import dev.anthonyhfm.amethyst.timeline.data.NoteGradientStop
import dev.anthonyhfm.amethyst.timeline.utils.GridUtils
import dev.anthonyhfm.amethyst.timeline.utils.resolveMidiClipTrimSpan
import dev.anthonyhfm.amethyst.timeline.viewport.EditorViewportState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EditorInteractionTest {
    private fun light(pad: Int, startMs: Long = 100L) = MidiNote.withPaint(
        device = 1,
        pitch = pad,
        color = Color.Red,
        startTimeMs = startMs,
        durationMs = 500L,
        gradient = listOf(
            NoteGradientStop(position = 0f, r = 1f, g = 0f, b = 0f),
            NoteGradientStop(position = 1f, r = 0f, g = 0f, b = 1f),
        ),
    )

    @Test
    fun movingAGroupAtTheTopKeepsItsShapeAndPaint() {
        val notes = listOf(light(pad = 98), light(pad = 99, startMs = 350L))
        val changes = movePianoRollNotes(
            notes = notes,
            timeDeltaMs = 200L,
            padDelta = 1,
            pads = (0..99).toList(),
        )
        assertEquals(listOf(98, 99), changes.map { it.after.pitch })
        assertEquals(listOf(300L, 550L), changes.map { it.after.startTimeMs })
        changes.forEach { change ->
            assertEquals(change.before.noteId, change.after.noteId)
            assertEquals(change.before.led.gradient, change.after.led.gradient)
            assertEquals(1, change.after.device)
            assertEquals(change.after.pitch, change.after.led.index)
        }
    }

    @Test
    fun movingBeforeZeroClampsTheGroupWithoutChangingItsSpacing() {
        val changes = movePianoRollNotes(
            notes = listOf(light(pad = 0), light(pad = 1, startMs = 350L)),
            timeDeltaMs = -500L,
            padDelta = -10,
            pads = (0..99).toList(),
        )
        assertEquals(listOf(0L, 250L), changes.map { it.after.startTimeMs })
        assertEquals(listOf(0, 1), changes.map { it.after.pitch })
    }

    @Test
    fun foldedPadMovementUsesVisibleRows() {
        val changes = movePianoRollNotes(
            notes = listOf(light(pad = 11), light(pad = 33)),
            timeDeltaMs = 0L,
            padDelta = 1,
            pads = listOf(11, 33, 88),
        )
        assertEquals(listOf(33, 88), changes.map { it.after.pitch })
    }

    @Test
    fun shortNotesStillHaveAMovableBody() {
        val note = light(pad = 11)
        val rect = PianoRollNoteRect(note = note, left = 0f, top = 0f, width = 6f, height = 22f)
        assertIs<PianoRollHitTarget.NoteBody>(
            findPianoRollHitTarget(point = Offset(x = 3f, y = 10f), noteRects = listOf(rect))
        )
        assertIs<PianoRollHitTarget.ResizeLeft>(
            findPianoRollHitTarget(point = Offset(x = 0.5f, y = 10f), noteRects = listOf(rect))
        )
        assertIs<PianoRollHitTarget.ResizeRight>(
            findPianoRollHitTarget(point = Offset(x = 5.5f, y = 10f), noteRects = listOf(rect))
        )
    }

    @Test
    fun overlappingNotesHitTheLastDrawnNote() {
        val bottom = light(pad = 11)
        val top = light(pad = 11)
        val rects = listOf(bottom, top).map { note ->
            PianoRollNoteRect(note = note, left = 0f, top = 0f, width = 80f, height = 22f)
        }
        val hit = assertIs<PianoRollHitTarget.NoteBody>(
            findPianoRollHitTarget(point = Offset(x = 40f, y = 10f), noteRects = rects)
        )
        assertEquals(top.noteId, hit.note.noteId)
    }

    @Test
    fun zoomNearTheEndKeepsTheAnchorTimeVisible() {
        val viewport = EditorViewportState(
            zoomX = 1f,
            scrollX = 1200f,
            viewportWidth = 800f,
            contentWidth = 2000f,
        )
        val zoomed = zoomPianoRollViewport(
            viewport = viewport,
            scaleDelta = 2f,
            anchorPx = 400f,
            contentDurationMs = 2000L,
        )
        assertEquals(viewport.screenToTimeMs(screenX = 400f), zoomed.screenToTimeMs(screenX = 400f))
        assertEquals(2800f, zoomed.scrollX)
        assertEquals(4000f, zoomed.contentWidth)
    }

    @Test
    fun midiTrimUsesTheSameSnappedAndBoundedEdgesForPreviewAndCommit() {
        val span = resolveMidiClipTrimSpan(
            startMs = 500L,
            endMs = 1500L,
            leftDeltaPx = 240f,
            rightDeltaPx = 0f,
            zoomX = 1f,
            snapTime = { GridUtils.snapToGrid(timeMs = it, zoomLevel = 1f, bpm = 120.0, gridType = GridUtils.GridType.Fixed._1_4) },
        )
        assertEquals(500L, span.startMs)
        assertEquals(1000L, span.durationMs)
        val bounded = resolveMidiClipTrimSpan(
            startMs = 500L,
            endMs = 1500L,
            leftDeltaPx = 2000f,
            rightDeltaPx = 0f,
            zoomX = 1f,
            snapTime = { it },
        )
        assertEquals(1450L, bounded.startMs)
        assertEquals(50L, bounded.durationMs)
    }

    @Test
    fun untouchedAndVeryShortClipsDoNotJumpOntoTheGrid() {
        val span = resolveMidiClipTrimSpan(
            startMs = 123L,
            endMs = 143L,
            leftDeltaPx = 0f,
            rightDeltaPx = 0f,
            zoomX = 1f,
            snapTime = { 0L },
        )
        assertEquals(123L, span.startMs)
        assertEquals(20L, span.durationMs)
    }

    @Test
    fun steppingThePianoRollGridLocksResolutionAtItsBoundaries() {
        val mode = PianoRollWorkspaceMode()
        mode.gridResolution = GridResolution.OneTwentyEighth
        assertTrue(mode.stepGrid(narrower = true))
        assertTrue(mode.gridResolutionLocked)
        assertEquals(GridResolution.OneTwentyEighth, mode.gridResolution)
        assertTrue(mode.stepGrid(narrower = false))
        assertEquals(GridResolution.SixtyFourth, mode.gridResolution)
    }

    @Test
    fun resizingUsesTheVisibleSixteenthLinesInsteadOfWholeBeats() {
        val note = light(pad = 11, startMs = 0L)
        val changes = resizePianoRollNotes(
            notes = listOf(note),
            anchorNote = note,
            fromLeft = false,
            timeDeltaMs = 130.0,
            resolution = GridResolution.Quarter,
            beatDurationMs = 500.0,
            snapEnabled = true,
        )
        assertEquals(note.copy(durationMs = 625L), changes.single().after)
    }

    @Test
    fun draggingMovesOnSmallLinesWhileDrawingKeepsTheCoarseGrid() {
        val deltaMs = pianoRollNoteMoveTimeDelta(
            anchorStartMs = 500L,
            timeDeltaMs = 130.0,
            resolution = GridResolution.Quarter,
            beatDurationMs = 500.0,
            snapEnabled = true,
        )
        assertEquals(125L, deltaMs)
        assertEquals(
            500L,
            floorClipTimeToGrid(
                clipTimeMs = 630.0,
                resolution = GridResolution.Quarter,
                beatDurationMs = 500.0,
            ),
        )
        val anchor = light(pad = 11, startMs = 500L)
        val other = light(pad = 22, startMs = 840L)
        val changes = movePianoRollNotes(
            notes = listOf(anchor, other),
            timeDeltaMs = deltaMs,
            padDelta = 0,
            pads = (0..99).toList(),
        )
        assertEquals(listOf(anchor.copy(startTimeMs = 625L), other.copy(startTimeMs = 965L)), changes.map { it.after })
    }

    @Test
    fun draggingRetainsFinerGridsAndProjectTempo() {
        assertEquals(
            150L,
            pianoRollNoteMoveTimeDelta(
                anchorStartMs = 600L,
                timeDeltaMs = 160.0,
                resolution = GridResolution.Eighth,
                beatDurationMs = 600.0,
                snapEnabled = true,
            ),
        )
        assertEquals(
            62L,
            pianoRollNoteMoveTimeDelta(
                anchorStartMs = 500L,
                timeDeltaMs = 65.0,
                resolution = GridResolution.ThirtySecond,
                beatDurationMs = 500.0,
                snapEnabled = true,
            ),
        )
    }

    @Test
    fun verticalDraggingKeepsOffGridTimesAndSnapBypassMovesFreely() {
        assertEquals(
            0L,
            pianoRollNoteMoveTimeDelta(
                anchorStartMs = 537L,
                timeDeltaMs = 0.0,
                resolution = GridResolution.Quarter,
                beatDurationMs = 500.0,
                snapEnabled = true,
            ),
        )
        assertEquals(
            37L,
            pianoRollNoteMoveTimeDelta(
                anchorStartMs = 537L,
                timeDeltaMs = 37.0,
                resolution = GridResolution.Quarter,
                beatDurationMs = 500.0,
                snapEnabled = false,
            ),
        )
    }

    @Test
    fun fineKeyboardStepsDoNotGetStuckAtFractionalMillisecondBoundaries() {
        listOf(500.0, millisecondsPerBeat(bpm = 137.0)).forEach { beatDurationMs ->
            var timeMs = 0L
            repeat(times = 16) { index ->
                timeMs = stepClipTimeOnGrid(
                    clipTimeMs = timeMs,
                    resolution = GridResolution.ThirtySecond,
                    direction = 1,
                    beatDurationMs = beatDurationMs,
                )
                assertEquals(((index + 1) * beatDurationMs / 8).toLong(), timeMs)
            }
            repeat(times = 16) { index ->
                timeMs = stepClipTimeOnGrid(
                    clipTimeMs = timeMs,
                    resolution = GridResolution.ThirtySecond,
                    direction = -1,
                    beatDurationMs = beatDurationMs,
                )
                assertEquals(((15 - index) * beatDurationMs / 8).toLong(), timeMs)
            }
        }
    }

    @Test
    fun resizingMultipleNotesUsesTheDraggedEdgeAndPreservesTheirOffsetsAndPaint() {
        val anchor = light(pad = 11, startMs = 0L)
        val other = light(pad = 22, startMs = 40L).copy(durationMs = 750L)
        val changes = resizePianoRollNotes(
            notes = listOf(anchor, other),
            anchorNote = anchor,
            fromLeft = false,
            timeDeltaMs = 130.0,
            resolution = GridResolution.Quarter,
            beatDurationMs = 500.0,
            snapEnabled = true,
        )
        assertEquals(listOf(anchor.copy(durationMs = 625L), other.copy(durationMs = 875L)), changes.map { it.after })
    }

    @Test
    fun resizingLeftEdgesKeepsEachEndFixedAndUsesProjectTempo() {
        val anchor = light(pad = 11, startMs = 600L)
        val other = light(pad = 22, startMs = 900L)
        val changes = resizePianoRollNotes(
            notes = listOf(anchor, other),
            anchorNote = anchor,
            fromLeft = true,
            timeDeltaMs = 160.0,
            resolution = GridResolution.Eighth,
            beatDurationMs = 600.0,
            snapEnabled = true,
        )
        assertEquals(listOf(750L, 1050L), changes.map { it.after.startTimeMs })
        assertEquals(listOf(350L, 350L), changes.map { it.after.durationMs })
        assertEquals(listOf(anchor.endTimeMs, other.endTimeMs), changes.map { it.after.endTimeMs })
    }

    @Test
    fun finerGridSettingsRemainAvailableWhenResizing() {
        val note = light(pad = 11, startMs = 0L)
        val changes = resizePianoRollNotes(
            notes = listOf(note),
            anchorNote = note,
            fromLeft = false,
            timeDeltaMs = 65.0,
            resolution = GridResolution.ThirtySecond,
            beatDurationMs = 500.0,
            snapEnabled = true,
        )
        assertEquals(562L, changes.single().after.endTimeMs)
    }

    @Test
    fun shorteningAGroupStopsAtItsShortestNoteWithoutExpandingShortNotes() {
        val anchor = light(pad = 11, startMs = 0L)
        val short = light(pad = 22, startMs = 250L).copy(durationMs = 200L)
        val changes = resizePianoRollNotes(
            notes = listOf(anchor, short),
            anchorNote = anchor,
            fromLeft = false,
            timeDeltaMs = -500.0,
            resolution = GridResolution.Quarter,
            beatDurationMs = 500.0,
            snapEnabled = true,
        )
        assertEquals(listOf(425L, 125L), changes.map { it.after.durationMs })
        val tiny = short.copy(durationMs = 20L)
        val tinyChanges = resizePianoRollNotes(
            notes = listOf(anchor, tiny),
            anchorNote = anchor,
            fromLeft = false,
            timeDeltaMs = -500.0,
            resolution = GridResolution.Quarter,
            beatDurationMs = 500.0,
            snapEnabled = true,
        )
        assertEquals(listOf(anchor, tiny), tinyChanges.map { it.after })
    }

    @Test
    fun leftResizeCannotMoveAnySelectedNoteBeforeZero() {
        val anchor = light(pad = 11, startMs = 500L)
        val earlier = light(pad = 22, startMs = 125L)
        val changes = resizePianoRollNotes(
            notes = listOf(anchor, earlier),
            anchorNote = anchor,
            fromLeft = true,
            timeDeltaMs = -500.0,
            resolution = GridResolution.Quarter,
            beatDurationMs = 500.0,
            snapEnabled = true,
        )
        assertEquals(listOf(375L, 0L), changes.map { it.after.startTimeMs })
        assertEquals(listOf(625L, 625L), changes.map { it.after.durationMs })
    }

    @Test
    fun resizingWithoutSnappingUsesTheMouseTimeAndNoMovementLeavesOffGridNotesAlone() {
        val note = light(pad = 11)
        val changes = resizePianoRollNotes(
            notes = listOf(note),
            anchorNote = note,
            fromLeft = false,
            timeDeltaMs = 37.0,
            resolution = GridResolution.Quarter,
            beatDurationMs = 500.0,
            snapEnabled = false,
        )
        assertEquals(note.copy(durationMs = 537L), changes.single().after)
        assertTrue(
            resizePianoRollNotes(
                notes = listOf(note),
                anchorNote = note,
                fromLeft = false,
                timeDeltaMs = 0.0,
                resolution = GridResolution.Quarter,
                beatDurationMs = 500.0,
                snapEnabled = true,
            ).isEmpty()
        )
    }
}
