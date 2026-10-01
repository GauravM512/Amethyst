package dev.anthonyhfm.amethyst

import androidx.compose.runtime.*
import dev.anthonyhfm.amethyst.home.Home
import dev.anthonyhfm.amethyst.ui.theme.ComposeAmethystTheme
import dev.anthonyhfm.amethyst.workspace.Workspace
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun App(externalWorkspaceOpenCount: Int = 0) {
    var inWorkspace by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = externalWorkspaceOpenCount) {
        if (externalWorkspaceOpenCount > 0) {
            inWorkspace = true
        }
    }

    if (inWorkspace) {
        Workspace(
            onBack = {
                WorkspaceRepository.clean()
                inWorkspace = false
            }
        )
    } else {
        Home(
            onOpenWorkspace = {
                inWorkspace = true
            }
        )
    }
}

@Composable
@Preview
private fun AppPreview() {
    ComposeAmethystTheme {
        App()
    }
}
