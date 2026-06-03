package dev.anthonyhfm.amethyst.start

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.amethyst_linux
import amethyst.composeapp.generated.resources.amethyst_windows
import amethyst.composeapp.generated.resources.amethyst_studio_logo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import dev.anthonyhfm.amethyst.desktop.DesktopPlatform
import dev.anthonyhfm.amethyst.desktop.OSXTitleBar
import dev.anthonyhfm.amethyst.desktop.utility.CenterWindowOnFirstShow
import dev.anthonyhfm.amethyst.ui.components.primitives.Button
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonVariant
import dev.anthonyhfm.amethyst.ui.components.primitives.DialogFooter
import dev.anthonyhfm.amethyst.ui.theme.AmethystTheme
import dev.anthonyhfm.amethyst.ui.theme.background
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.foreground
import dev.anthonyhfm.amethyst.ui.theme.h3
import dev.anthonyhfm.amethyst.ui.theme.mutedForeground
import dev.anthonyhfm.amethyst.ui.theme.mutedText
import dev.anthonyhfm.amethyst.ui.theme.p
import dev.anthonyhfm.amethyst.ui.theme.typography
import org.jetbrains.compose.resources.painterResource
import kotlin.system.exitProcess
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme

@Composable
fun EarlyAccessWindow(
    onAccept: () -> Unit,
    onCancel: () -> Unit,
) {
    Window(
        onCloseRequest = {
            exitProcess(0)
        },
        title = "Amethyst - Early Access",
        state = rememberWindowState(
            width = 480.dp,
            height = 380.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        ),
        icon = when (DesktopPlatform.get()) {
            DesktopPlatform.Windows -> painterResource(Res.drawable.amethyst_windows)
            DesktopPlatform.Linux -> painterResource(Res.drawable.amethyst_linux)
            else -> null
        },
        resizable = false
    ) {
        CenterWindowOnFirstShow(window)

        LaunchedEffect(Unit) {
            window.minimumSize = java.awt.Dimension(480, 380)
        }

        if (DesktopPlatform.get() == DesktopPlatform.MacOS) {
            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
            window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
        }

        AmethystTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Theme[colors][background])
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (DesktopPlatform.get() == DesktopPlatform.MacOS) {
                        OSXTitleBar()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.amethyst_studio_logo),
                            contentDescription = "Amethyst Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(64.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Warning: Early Access!",
                            style = Theme[typography][h3].copy(color = Theme[colors][foreground]),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Expect things to break, bugs to appear, and everything to go haywire. You have been warned!",
                            style = Theme[typography][p].copy(color = Theme[colors][foreground]),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "(Sarcasm aside: Amethyst is in an early stage of development, so you might encounter some bugs. Please use with patience and report issues constructively!)",
                            style = Theme[typography][mutedText].copy(color = Theme[colors][mutedForeground]),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(28.dp))

                        DialogFooter {
                            Button(
                                onClick = onCancel,
                                variant = ButtonVariant.Outline,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = onAccept,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Start Amethyst")
                            }
                        }
                    }
                }
            }
        }
    }
}
