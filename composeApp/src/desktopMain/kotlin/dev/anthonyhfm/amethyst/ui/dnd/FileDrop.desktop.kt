package dev.anthonyhfm.amethyst.ui.dnd

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import io.github.vinceglb.filekit.PlatformFile

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.fileDropTarget(
    onHover: (isHovering: Boolean, offset: Offset?, files: List<PlatformFile>) -> Unit,
    onDrop: (offset: Offset?, files: List<PlatformFile>) -> Unit
): Modifier {
    var isDragOver by remember { mutableStateOf(false) }
    var targetBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val currentOnHover by rememberUpdatedState(newValue = onHover)
    val currentOnDrop by rememberUpdatedState(newValue = onDrop)

    fun toLocalOffset(event: DragAndDropEvent): Offset? {
        val point = when (val ne = event.nativeEvent) {
            is java.awt.dnd.DropTargetDragEvent -> ne.location
            is java.awt.dnd.DropTargetDropEvent -> ne.location
            else -> null
        } ?: return null

        return fileDropLocalOffset(
            positionInRoot = Offset(x = point.x.toFloat(), y = point.y.toFloat()),
            targetBoundsInRoot = targetBoundsInRoot
        )
    }

    fun isInsideTarget(offset: Offset?): Boolean =
        offset != null &&
            offset.x >= 0f && offset.x < targetBoundsInRoot.width &&
            offset.y >= 0f && offset.y < targetBoundsInRoot.height

    return this
        .onGloballyPositioned { coordinates ->
            targetBoundsInRoot = coordinates.boundsInRoot()
        }
        .dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                try {
                    val transferable = event.awtTransferable
                    supportsFileDrop(transferable = transferable)
                } catch (_: Exception) {
                    false
                }
            },
            target = remember {
                object : DragAndDropTarget {
                    override fun onStarted(event: DragAndDropEvent) {
                        isDragOver = false
                    }

                    override fun onEntered(event: DragAndDropEvent) {
                        onMoved(event = event)
                    }

                    override fun onMoved(event: DragAndDropEvent) {
                        val local = toLocalOffset(event = event)
                        if (isInsideTarget(offset = local)) {
                            isDragOver = true
                            val files = getEventFiles(event = event)
                            currentOnHover(true, local, files)
                        } else if (isDragOver) {
                            isDragOver = false
                            currentOnHover(false, null, emptyList())
                        }
                    }

                    override fun onExited(event: DragAndDropEvent) {
                        clearHover()
                    }

                    override fun onEnded(event: DragAndDropEvent) {
                        clearHover()
                    }

                    private fun clearHover() {
                        isDragOver = false
                        currentOnHover(false, null, emptyList())
                    }

                    override fun onDrop(event: DragAndDropEvent): Boolean {
                        val local = toLocalOffset(event = event)
                        clearHover()
                        if (!isInsideTarget(offset = local)) {
                            return false
                        }
                        val files = getEventFiles(event = event)
                        if (files.isEmpty()) {
                            return false
                        }
                        currentOnDrop(local, files)
                        return true
                    }
                }
            }
        )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun getEventFiles(event: DragAndDropEvent): List<PlatformFile> {
    return try {
        readDroppedFiles(transferable = event.awtTransferable).map { file ->
            PlatformFile(file = file)
        }
    } catch (_: Exception) {
        emptyList()
    }
}
