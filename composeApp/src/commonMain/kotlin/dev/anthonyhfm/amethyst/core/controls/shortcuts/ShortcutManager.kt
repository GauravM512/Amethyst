package dev.anthonyhfm.amethyst.core.controls.shortcuts

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import dev.anthonyhfm.amethyst.core.controls.clipboard.ClipboardManager
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.controls.selection.Selectable
import dev.anthonyhfm.amethyst.core.controls.undo.UndoManager
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository

object ShortcutManager {
    fun handleShortcut(keyEvent: KeyEvent): Boolean {
        if (keyEvent.type != KeyEventType.KeyDown) return false

        // Undo/Redo Shortcuts
        if (keyEvent.isCtrlPressed && keyEvent.key == Key.Z) {
            return if (keyEvent.isShiftPressed) {
                // Ctrl+Shift+Z = Redo
                UndoManager.redo()
                true
            } else {
                // Ctrl+Z = Undo
                UndoManager.undo()
                true
            }
        }

        // Alternative Redo shortcut (Ctrl+Y)
        if (keyEvent.isCtrlPressed && keyEvent.key == Key.Y) {
            UndoManager.redo()
            return true
        }

        if (keyEvent.key == Key.Backspace || keyEvent.key == Key.Delete) {
            return handleDeletionShortcut()
        }

        if (keyEvent.isCtrlPressed && keyEvent.key == Key.D) {
            return handleDuplicateShortcut()
        }

        if (keyEvent.isCtrlPressed && keyEvent.key == Key.C) {
            if (SelectionManager.selections.value.isNotEmpty()) {
                ClipboardManager.copy(SelectionManager.selections.value)
                return true
            }
        }

        if (keyEvent.isCtrlPressed && keyEvent.key == Key.V) {
            ClipboardManager.paste()
            return true
        }

        if (keyEvent.isCtrlPressed && keyEvent.key == Key.S) {
            println("TODO: Save")
            return true
        }

        if (keyEvent.key == Key.DirectionDown || keyEvent.key == Key.DirectionUp) {
            return handleNavigationShortcut(keyEvent)
        }

        return false
    }

    private fun handleDeletionShortcut(): Boolean {
        val selections = SelectionManager.selections.value
        if (selections.isEmpty()) return false

        selections.forEach { selectable ->
            when (selectable) {
                is Selectable.ChainDevice -> {
                    selectable.parent.remove(selectable.device.selectionUUID, fromUser = true)
                }
                is Selectable.VirtualViewportDevice -> {
                    // TODO: Implement virtual viewport device deletion
                    println("Virtual Viewport Device deletion not yet implemented")
                }
                is Selectable.GradientStep -> {
                    // TODO: Implement gradient step deletion
                    println("Gradient Step deletion not yet implemented")
                }
                is Selectable.GroupChainItem -> {
                    // TODO: Implement group chain item deletion
                    println("Group Chain Item deletion not yet implemented")
                }
                is Selectable.KeyframeItem -> {
                    // TODO: Implement keyframe item deletion
                    println("Keyframe Item deletion not yet implemented")
                }
            }
        }

        SelectionManager.clear()
        return true
    }

    private fun handleDuplicateShortcut(): Boolean {
        val selections = SelectionManager.selections.value
        if (selections.isEmpty()) return false

        // Kopieren und sofort einfügen
        ClipboardManager.copy(selections)
        ClipboardManager.paste()
        return true
    }

    private fun handleNavigationShortcut(keyEvent: KeyEvent): Boolean {
        val selections = SelectionManager.selections.value
        if (selections.isEmpty()) return false

        // TODO: Implement navigation shortcuts (move selection up/down in chains)
        when (keyEvent.key) {
            Key.DirectionUp -> {
                println("TODO: Move selection up")
            }
            Key.DirectionDown -> {
                println("TODO: Move selection down")
            }
        }

        return true
    }
}