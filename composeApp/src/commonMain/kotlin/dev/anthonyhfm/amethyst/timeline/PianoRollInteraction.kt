package dev.anthonyhfm.amethyst.timeline

import androidx.compose.ui.geometry.Offset
import dev.anthonyhfm.amethyst.timeline.contract.GridResolution
import dev.anthonyhfm.amethyst.timeline.data.MidiNote
import dev.anthonyhfm.amethyst.timeline.data.NoteGradientStop
import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.timeline.data.resolvedDeviceIndex
import dev.anthonyhfm.amethyst.timeline.data.resolvedPadIndex
import dev.anthonyhfm.amethyst.timeline.viewport.EditorViewportState
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal sealed interface PianoRollHitTarget {
    data object Empty : PianoRollHitTarget
    data class NoteBody(val note: MidiNote) : PianoRollHitTarget
    data class ResizeLeft(val note: MidiNote) : PianoRollHitTarget
    data class ResizeRight(val note: MidiNote) : PianoRollHitTarget
}

internal data class PianoRollNoteRect(
    val note: MidiNote,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float
        get() = left + width

    val bottom: Float
        get() = top + height

    fun contains(point: Offset): Boolean =
        point.x in left..right && point.y in top..bottom
}

internal data class PianoRollNoteLayout(
    val leftPx: Float,
    val widthPx: Float,
    val fullWidthPx: Float,
    val hiddenLeftPx: Float,
    val isLeftEdgeVisible: Boolean,
    val isRightEdgeVisible: Boolean,
) {
    fun gradientPositionAt(xPx: Float): Float =
        ((hiddenLeftPx + xPx) / fullWidthPx).coerceIn(0f, 1f)

    fun gradientStopX(position: Float): Float = position * fullWidthPx - hiddenLeftPx
}

internal fun resolvePianoRollNoteLayout(
    screenStartPx: Double,
    screenEndPx: Double,
    viewportWidthPx: Float,
): PianoRollNoteLayout? {
    val normalizedEndPx = screenEndPx.coerceAtLeast(screenStartPx + 6.0)
    val leftPx = screenStartPx.coerceAtLeast(0.0)
    val rightPx = normalizedEndPx.coerceAtMost(viewportWidthPx.toDouble())
    if (rightPx <= leftPx) {
        return null
    }
    return PianoRollNoteLayout(
        leftPx = leftPx.toFloat(),
        widthPx = (rightPx - leftPx).toFloat(),
        fullWidthPx = (normalizedEndPx - screenStartPx).toFloat(),
        hiddenLeftPx = (leftPx - screenStartPx).toFloat(),
        isLeftEdgeVisible = screenStartPx >= 0.0,
        isRightEdgeVisible = normalizedEndPx <= viewportWidthPx,
    )
}

internal data class PianoRollDraftSpan(
    val startTimeMs: Long,
    val durationMs: Long,
)

internal data class PianoRollGridPoint(
    val deviceIndex: Int,
    val pointInDevice: Offset,
)

/** Maps a pointer in the scrolled editor viewport to one device's 0..99 pad grid. */
internal fun resolvePianoRollGridPoint(
    point: Offset,
    verticalScrollPx: Float,
    deviceHeaderHeightPx: Float,
    deviceRowHeightPx: Float,
    deviceCount: Int,
): PianoRollGridPoint? {
    if (deviceCount <= 0 || deviceRowHeightPx <= 0f) return null
    val contentY = point.y + verticalScrollPx
    val blockHeight = deviceHeaderHeightPx + deviceRowHeightPx
    if (contentY < 0f || blockHeight <= 0f) return null

    val deviceIndex = floor(contentY / blockHeight).toInt()
    if (deviceIndex !in 0 until deviceCount) return null
    val yInBlock = contentY - deviceIndex * blockHeight
    if (yInBlock < deviceHeaderHeightPx || yInBlock >= blockHeight) return null

    return PianoRollGridPoint(
        deviceIndex = deviceIndex,
        pointInDevice = Offset(point.x, yInBlock - deviceHeaderHeightPx),
    )
}

internal fun applyPianoRollNoteEdits(
    notes: List<MidiNote>,
    changes: List<TimelineEditedNote>,
): List<MidiNote> {
    val replacements = changes.associate { it.before.noteId to it.after }
    return notes.map { note -> replacements[note.noteId] ?: note }
}

internal fun findPianoRollHitTarget(
    point: Offset,
    noteRects: List<PianoRollNoteRect>,
    resizeHandleWidthPx: Float = 6f,
): PianoRollHitTarget {
    val hitRect = noteRects.lastOrNull { it.contains(point) } ?: return PianoRollHitTarget.Empty
    val handleWidth = pianoRollResizeHandleWidthPx(
        noteWidthPx = hitRect.width,
        preferredWidthPx = resizeHandleWidthPx,
    )
    return when {
        point.x <= hitRect.left + handleWidth -> PianoRollHitTarget.ResizeLeft(hitRect.note)
        point.x >= hitRect.right - handleWidth -> PianoRollHitTarget.ResizeRight(hitRect.note)
        else -> PianoRollHitTarget.NoteBody(hitRect.note)
    }
}

internal fun pianoRollResizeHandleWidthPx(
    noteWidthPx: Float,
    preferredWidthPx: Float,
): Float = minOf(preferredWidthPx.coerceAtLeast(0f), noteWidthPx.coerceAtLeast(0f) / 4f)

internal fun movePianoRollNotes(
    notes: List<MidiNote>,
    timeDeltaMs: Long,
    padDelta: Int,
    pads: List<Int>,
): List<TimelineEditedNote> {
    if (notes.isEmpty() || pads.isEmpty()) {
        return emptyList()
    }
    val indices = notes.map { pads.indexOf(it.resolvedPadIndex) }
    if (indices.any { it < 0 }) {
        return emptyList()
    }
    val boundedTimeDelta = timeDeltaMs.coerceAtLeast(-notes.minOf { it.startTimeMs })
    val boundedPadDelta = padDelta.coerceIn(-indices.min(), pads.lastIndex - indices.max())
    return notes.mapIndexed { index, note ->
        val pad = pads[indices[index] + boundedPadDelta]
        TimelineEditedNote(
            before = note,
            after = note.copy(
                startTimeMs = note.startTimeMs + boundedTimeDelta,
                device = note.resolvedDeviceIndex,
                pitch = pad,
                led = note.led.copy(index = pad),
            ),
        )
    }
}

internal fun pianoRollNoteEditResolution(resolution: GridResolution): GridResolution =
    if (resolution.snapDivisionsPerBeat < GridResolution.Sixteenth.snapDivisionsPerBeat) {
        GridResolution.Sixteenth
    } else {
        resolution
    }

internal fun pianoRollNoteMoveTimeDelta(
    anchorStartMs: Long,
    timeDeltaMs: Double,
    resolution: GridResolution,
    beatDurationMs: Double,
    snapEnabled: Boolean,
): Long {
    if (timeDeltaMs == 0.0) {
        return 0L
    }
    val targetStartMs = if (snapEnabled) {
        snapClipTimeToGrid(
            clipTimeMs = anchorStartMs + timeDeltaMs,
            resolution = pianoRollNoteEditResolution(resolution = resolution),
            beatDurationMs = beatDurationMs,
        )
    } else {
        (anchorStartMs + timeDeltaMs).roundToLong()
    }
    return targetStartMs - anchorStartMs
}

internal fun resizePianoRollNotes(
    notes: List<MidiNote>,
    anchorNote: MidiNote,
    fromLeft: Boolean,
    timeDeltaMs: Double,
    resolution: GridResolution,
    beatDurationMs: Double,
    snapEnabled: Boolean,
): List<TimelineEditedNote> {
    if (notes.isEmpty() || timeDeltaMs == 0.0) {
        return emptyList()
    }
    val resizeResolution = pianoRollNoteEditResolution(resolution = resolution)
    val anchorEdgeMs = if (fromLeft) anchorNote.startTimeMs else anchorNote.endTimeMs
    val requestedEdgeMs = if (snapEnabled) {
        snapClipTimeToGrid(
            clipTimeMs = anchorEdgeMs + timeDeltaMs,
            resolution = resizeResolution,
            beatDurationMs = beatDurationMs,
        )
    } else {
        (anchorEdgeMs + timeDeltaMs).roundToLong()
    }
    val minimumCellMs = if (snapEnabled) {
        (beatDurationMs / resizeResolution.snapDivisionsPerBeat).toLong().coerceAtLeast(1L)
    } else {
        1L
    }
    val maximumShrinkMs = notes.minOf { note ->
        note.durationMs - minOf(note.durationMs.coerceAtLeast(1L), minimumCellMs)
    }
    val requestedDeltaMs = requestedEdgeMs - anchorEdgeMs
    val boundedDeltaMs = if (fromLeft) {
        requestedDeltaMs.coerceIn(-notes.minOf { it.startTimeMs }, maximumShrinkMs)
    } else {
        requestedDeltaMs.coerceAtLeast(-maximumShrinkMs)
    }
    return notes.map { note ->
        TimelineEditedNote(
            before = note,
            after = if (fromLeft) {
                note.copy(
                    startTimeMs = note.startTimeMs + boundedDeltaMs,
                    durationMs = note.durationMs - boundedDeltaMs,
                )
            } else {
                note.copy(durationMs = note.durationMs + boundedDeltaMs)
            },
        )
    }
}

internal fun stepPianoRollPadNotes(
    notes: List<MidiNote>,
    pads: List<Pair<Int, Int>>,
    currentMs: Long,
    nextMs: Long,
    color: Color,
    gradient: List<NoteGradientStop>?,
): List<MidiNote> {
    if (nextMs == currentMs) {
        return notes
    }
    val updatedNotes = notes.toMutableList()
    pads.distinct().forEach { (deviceIndex, padIndex) ->
        val index = updatedNotes.indexOfLast { note ->
            val samePad = note.resolvedDeviceIndex == deviceIndex && note.resolvedPadIndex == padIndex
            if (nextMs < currentMs) {
                samePad && note.startTimeMs < currentMs && note.endTimeMs >= currentMs
            } else {
                val samePaint = if (gradient != null) {
                    note.led.gradient?.map { listOf(it.position, it.r, it.g, it.b) } ==
                        gradient.map { listOf(it.position, it.r, it.g, it.b) }
                } else {
                    note.led.gradient == null && note.led.red == color.red &&
                        note.led.green == color.green && note.led.blue == color.blue
                }
                samePad && samePaint && note.startTimeMs <= currentMs && note.endTimeMs >= currentMs
            }
        }
        val note = updatedNotes.getOrNull(index)
        if (nextMs < currentMs) {
            if (note != null) {
                if (nextMs <= note.startTimeMs) {
                    updatedNotes.removeAt(index = index)
                } else {
                    updatedNotes[index] = note.copy(durationMs = nextMs - note.startTimeMs)
                }
            }
        } else if (note != null) {
            updatedNotes[index] = note.copy(durationMs = maxOf(note.endTimeMs, nextMs) - note.startTimeMs)
        } else {
            updatedNotes.add(
                MidiNote.withPaint(
                    device = deviceIndex,
                    pitch = padIndex,
                    color = color,
                    startTimeMs = currentMs,
                    durationMs = nextMs - currentMs,
                    gradient = gradient,
                )
            )
        }
    }
    return updatedNotes
}

internal fun resolveDraftSpan(
    anchorCellStartMs: Long,
    currentCellStartMs: Long,
    cellDurationMs: Long,
): PianoRollDraftSpan {
    val safeCellDurationMs = cellDurationMs.coerceAtLeast(1L)
    val startTimeMs = min(anchorCellStartMs, currentCellStartMs)
    val endTimeMs = max(anchorCellStartMs, currentCellStartMs) + safeCellDurationMs
    return PianoRollDraftSpan(
        startTimeMs = startTimeMs,
        durationMs = (endTimeMs - startTimeMs).coerceAtLeast(safeCellDurationMs),
    )
}

/**
 * Returns a viewport-relative X coordinate for the cursor.
 *
 * Both [trackedPointerX] (from the move-tracking pointerInput) and [eventPointerX] (from the
 * scroll event) are in the VIEWPORT coordinate system because neither pointer handler sits inside
 * a horizontalScroll modifier anymore.  The preferred value is [trackedPointerX] because it was
 * last updated on a pointer-move event and therefore leads to more accurate anchoring.
 */
internal fun resolveViewportRelativeCursorX(
    trackedPointerX: Float?,
    eventPointerX: Float?,
): Float = trackedPointerX ?: (eventPointerX ?: 0f)

// ── Viewport-aware snapping ───────────────────────────────────────────────────

/**
 * Snaps [clipTimeMs] to the nearest grid boundary defined by [resolution].
 *
 * Uses the supplied project-tempo beat duration so the result aligns with the visual grid.
 */
internal fun snapClipTimeToGrid(
    clipTimeMs: Double,
    resolution: GridResolution,
    beatDurationMs: Double = DEFAULT_MS_PER_BEAT,
): Long {
    val n = resolution.snapDivisionsPerBeat
    val beatFraction = clipTimeMs / beatDurationMs
    val snapped = kotlin.math.round(beatFraction * n) / n.toDouble()
    return (snapped * beatDurationMs).toLong()
}

/**
 * Floors [clipTimeMs] to the start of the grid cell containing it.
 *
 * Used by the DRAW tool so a new note anchors to the cell the user clicked,
 * matching the floor behaviour of [PianoRollMetrics.xPxToNotePlacementMs].
 */
internal fun floorClipTimeToGrid(
    clipTimeMs: Double,
    resolution: GridResolution,
    beatDurationMs: Double = DEFAULT_MS_PER_BEAT,
): Long {
    val n = resolution.snapDivisionsPerBeat
    val beatFraction = clipTimeMs / beatDurationMs
    val floored = floor(beatFraction * n) / n.toDouble()
    return (floored * beatDurationMs).toLong()
}

internal fun cellDurationAt(
    cellStartMs: Long,
    resolution: GridResolution,
    beatDurationMs: Double = DEFAULT_MS_PER_BEAT,
): Long {
    val n = resolution.snapDivisionsPerBeat
    val currentBeatFraction = cellStartMs / beatDurationMs
    val k = kotlin.math.round(currentBeatFraction * n)
    val nextCellStartMs = (((k + 1) * beatDurationMs) / n.toDouble()).toLong()
    return nextCellStartMs - cellStartMs
}

/**
 * Moves [clipTimeMs] by exactly one grid cell of [resolution] in the given [direction]
 * (`+1` = forward/right, `-1` = backward/left), always landing exactly on a grid boundary —
 * even if [clipTimeMs] itself was off-grid to begin with.
 *
 * Used to grid-align keyboard-driven (arrow key) playhead nudging in the piano roll,
 * matching the same tempo-aware grid the renderer and note-drawing tools use.
 */
internal fun stepClipTimeOnGrid(
    clipTimeMs: Long,
    resolution: GridResolution,
    direction: Int,
    beatDurationMs: Double = DEFAULT_MS_PER_BEAT,
): Long {
    val n = resolution.snapDivisionsPerBeat
    val beatFraction = clipTimeMs / beatDurationMs
    val rawK = beatFraction * n
    val nearestK = kotlin.math.round(rawK)
    val nearestTimeMs = (nearestK * beatDurationMs / n.toDouble()).toLong()
    val k = if (clipTimeMs == nearestTimeMs) nearestK else rawK
    val epsilon = 1e-6
    val steppedK = if (direction >= 0) {
        floor(k + epsilon) + 1
    } else {
        ceil(k - epsilon) - 1
    }
    return ((steppedK / n.toDouble()) * beatDurationMs).toLong()
}

/**
 * Builds [PianoRollNoteRect] instances in **screen-space** (viewport coordinates).
 *
 * Use when the pointer-input handler is attached to the outer viewport box so that
 * pointer-event positions (screen-space) and note rectangles share the same coordinate
 * space, making hit-testing reliable and consistent with the renderer's viewport model.
 */
internal fun buildNoteRectsScreenSpace(
    notes: List<MidiNote>,
    metrics: PianoRollMetrics,
    viewport: EditorViewportState,
): List<PianoRollNoteRect> = notes.map { note ->
    PianoRollNoteRect(
        note = note,
        left = viewport.clipTimeMsToScreenX(clipTimeMs = note.startTimeMs.toDouble(), oobOffsetMs = metrics.oobOffsetMs),
        top = metrics.pitchToYPx(note.resolvedPadIndex),
        width = metrics.durationMsToWidthPx(note.durationMs).coerceAtLeast(6f),
        height = metrics.noteRenderHeightPx,
    )
}

internal fun zoomPianoRollViewport(
    viewport: EditorViewportState,
    scaleDelta: Float,
    anchorPx: Float,
    contentDurationMs: Long,
): EditorViewportState {
    val zoomX = (viewport.zoomX * scaleDelta).coerceIn(viewport.minZoomX, viewport.maxZoomX)
    val anchorTimeMs = viewport.screenToTimeMs(screenX = anchorPx)
    return viewport.withConstrainedViewport(
        zoomX = zoomX,
        scrollX = (anchorTimeMs * zoomX - anchorPx).toFloat(),
        contentWidth = zoomX * contentDurationMs,
    )
}
