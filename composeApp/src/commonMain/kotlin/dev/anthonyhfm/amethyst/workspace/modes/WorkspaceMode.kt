package dev.anthonyhfm.amethyst.workspace.modes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import dev.anthonyhfm.amethyst.core.midi.data.MidiInputData
import dev.anthonyhfm.amethyst.workspace.modes.defaults.LayoutWorkspaceMode
import dev.anthonyhfm.amethyst.workspace.modes.defaults.PerformanceWorkspaceMode

abstract class WorkspaceMode {
    abstract val displayName: String

    open val claimMidiInputs: Boolean = false
    open val selectableMode: Boolean = false

    val availableInSimpleMode: Boolean
        get() = this is PerformanceWorkspaceMode || this is LayoutWorkspaceMode

    open fun onActivate() = Unit
    open fun onDeactivate() = Unit

    abstract fun onKeyEvent(event: KeyEvent): Boolean
    abstract fun onMidiInput(data: MidiInputData, offset: Offset)

    @Composable
    abstract fun Content(modifier: Modifier = Modifier)

    companion object
}
