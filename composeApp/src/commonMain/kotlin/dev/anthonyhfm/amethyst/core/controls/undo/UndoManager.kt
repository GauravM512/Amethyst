package dev.anthonyhfm.amethyst.core.controls.undo

object UndoManager {
    private val undoStack: MutableList<UndoableAction> = mutableListOf()
    private val redoStack: MutableList<UndoableAction> = mutableListOf()

    fun addAction(action: UndoableAction) {
        undoStack.add(action)
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val action = undoStack.removeAt(undoStack.lastIndex)

            when (action) {
                is UndoableAction.ChainDeviceCreation -> {
                    val deviceIndex = action.parent.devices.value.indexOfFirst {
                        it.selectionUUID == action.device.selectionUUID
                    }
                    if (deviceIndex != -1) {
                        action.parent.remove(deviceIndex, fromUser = false)
                    }
                    redoStack.add(action)
                }

                is UndoableAction.ChainDeviceRemoval -> {
                    action.parent.add(action.device, fromUser = false)
                    redoStack.add(action)
                }

                is UndoableAction.MovedChainDevice -> {
                    action.chainAfter.remove(action.device.selectionUUID, fromUser = false)

                    if (action.fromIndex >= 0 && action.fromIndex <= action.chainBefore.devices.value.size) {
                        action.chainBefore.add(action.device, action.fromIndex, fromUser = false)
                    } else {
                        action.chainBefore.add(action.device, fromUser = false)
                    }

                    redoStack.add(action)
                }
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val action = redoStack.removeAt(redoStack.lastIndex)

            when (action) {
                is UndoableAction.ChainDeviceCreation -> {
                    action.parent.add(action.device, fromUser = false)
                    undoStack.add(action)
                }

                is UndoableAction.ChainDeviceRemoval -> {
                    val deviceIndex = action.parent.devices.value.indexOfFirst {
                        it.selectionUUID == action.device.selectionUUID
                    }
                    if (deviceIndex != -1) {
                        action.parent.remove(deviceIndex, fromUser = false)
                    }
                    undoStack.add(action)
                }

                is UndoableAction.MovedChainDevice -> {
                    action.chainBefore.remove(action.device.selectionUUID, fromUser = false)
                    action.chainAfter.add(action.device, action.toIndex, fromUser = false)
                    undoStack.add(action)
                }
            }
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}