package dev.anthonyhfm.amethyst

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.anthonyhfm.amethyst.desktop.DesktopPlatform
import dev.anthonyhfm.amethyst.desktop.OSXTitleBar
import dev.anthonyhfm.amethyst.start.StartWindow

fun main() {
    val platform = DesktopPlatform.get()

    if (platform == DesktopPlatform.MacOS) {
        System.setProperty("apple.awt.application.name", "Amethyst")
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")
    }

    application {
        var showEditor: Boolean by remember { mutableStateOf(false) }

        if (!showEditor) {
            StartWindow(
                onOpenEditor = {
                    showEditor = true
                }
            )
        } else {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Amethyst",
                state = rememberWindowState(
                    width = 1200.dp,
                    height = 800.dp
                )
            ) {
                if(platform == DesktopPlatform.MacOS) {
                    window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                    window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                }

                MaterialTheme(
                    colorScheme = darkColorScheme()
                ) {
                    Column {
                        if (platform == DesktopPlatform.MacOS) {
                            OSXTitleBar()
                        }

                        App()
                    }
                }
            }
        }
    }
}