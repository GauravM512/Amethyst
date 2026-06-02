package dev.anthonyhfm.amethyst.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository

fun Modifier.trackInputFocus(): Modifier = this.onFocusChanged { focusState ->
    WorkspaceRepository.isInputFocused = focusState.isFocused
}
