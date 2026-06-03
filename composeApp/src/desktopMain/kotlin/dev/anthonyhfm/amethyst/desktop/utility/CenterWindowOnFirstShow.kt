package dev.anthonyhfm.amethyst.desktop.utility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.yield
import java.awt.Window
import javax.swing.SwingUtilities

private const val MAX_CENTER_WAIT_ITERATIONS = 8

@Composable
fun CenterWindowOnFirstShow(window: Window) {
    LaunchedEffect(window) {
        var attempts = 0
        while ((!window.isShowing || window.width <= 0 || window.height <= 0) &&
            attempts < MAX_CENTER_WAIT_ITERATIONS
        ) {
            yield()
            attempts++
        }

        SwingUtilities.invokeLater {
            if (window.isDisplayable) {
                window.setLocationRelativeTo(null)
            }
        }
    }
}
