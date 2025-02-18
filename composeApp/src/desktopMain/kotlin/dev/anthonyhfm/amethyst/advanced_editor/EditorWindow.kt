package dev.anthonyhfm.amethyst.advanced_editor

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import dev.anthonyhfm.amethyst.desktop.DesktopPlatform
import dev.anthonyhfm.amethyst.desktop.OSXTitleBar
import dev.anthonyhfm.amethyst.advanced_editor.ui.EditorMenuBar
import kotlin.system.exitProcess

@Composable
fun EditorWindow() {
    Window(
        onCloseRequest = {
            exitProcess(0)
        },
        title = "Amethyst",
        state = rememberWindowState(
            width = 1200.dp,
            height = 800.dp
        )
    ) {
        EditorMenuBar()

        LaunchedEffect(Unit) {
            if(DesktopPlatform.get() == DesktopPlatform.MacOS) {
                window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
            }
        }

        MaterialTheme(
            colorScheme = darkColorScheme()
        ) {
            Column {
                if (DesktopPlatform.get() == DesktopPlatform.MacOS) {
                    OSXTitleBar()
                }

                AdvancedEditor()
            }
        }
    }
}