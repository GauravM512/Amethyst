package dev.anthonyhfm.amethyst.workspace.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FrameWindowScope.WorkspaceMenuBar() {
    val viewModel = viewModel { WorkspaceMenuBarViewModel() }

    MenuBar {
        Menu(
            text = "File"
        ) {

        }

        Menu(
            text = "Edit"
        ) {

        }

        Menu(
            text = "View"
        ) {

        }
    }
}