package dev.anthonyhfm.amethyst

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import dev.anthonyhfm.amethyst.home.Home
import dev.anthonyhfm.amethyst.ui.theme.AMETHYST_THEME
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = AMETHYST_THEME
    ) {
        Surface {
            Home()
        }
    }
}