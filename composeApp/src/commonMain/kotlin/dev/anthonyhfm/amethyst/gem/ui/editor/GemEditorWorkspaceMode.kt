package dev.anthonyhfm.amethyst.gem.ui.editor

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import dev.anthonyhfm.amethyst.workspace.WorkspaceContract

class GemEditorWorkspaceMode(
    override val displayName: String = "Gem Editor",
    override val selectable: Boolean = false
) : WorkspaceContract.WorkspaceMode {
    override fun onKeyEvent(event: KeyEvent): Boolean {
        return super.onKeyEvent(event)
    }

    @Composable
    fun ModeContent(paddingValues: PaddingValues) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {

        }
    }
}