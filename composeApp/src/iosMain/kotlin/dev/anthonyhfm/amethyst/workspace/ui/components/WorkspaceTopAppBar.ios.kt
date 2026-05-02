package dev.anthonyhfm.amethyst.workspace.ui.components

import androidx.compose.runtime.Composable
import dev.anthonyhfm.amethyst.workspace.WorkspaceContract

@Composable
actual fun WorkspaceTopAppBar(
    onBack: () -> Unit,
    mode: WorkspaceContract.WorkspaceMode,
    onEvent: (WorkspaceContract.Event) -> Unit,
) {
    // iOS top app bar — to be implemented
}
