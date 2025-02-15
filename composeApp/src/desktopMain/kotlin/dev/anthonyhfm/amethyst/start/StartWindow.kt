package dev.anthonyhfm.amethyst.start

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import dev.anthonyhfm.amethyst.desktop.DesktopPlatform
import dev.anthonyhfm.amethyst.start.ui.AmethystWelcome
import dev.anthonyhfm.amethyst.ui.modifier.platformPaddingTop

@Composable
fun StartWindow(
    exitApplication: () -> Unit,
) {
    Window(
        onCloseRequest = exitApplication,
        title = "Amethyst",
        state = rememberWindowState(
            width = 700.dp,
            height = 450.dp
        ),
    ) {
        if(DesktopPlatform.get() == DesktopPlatform.MacOS) {
            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
            window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
        }

        MaterialTheme(
            colorScheme = darkColorScheme()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .platformPaddingTop()
                ) {
                    AmethystWelcome()
                }
            }
        }
    }
}