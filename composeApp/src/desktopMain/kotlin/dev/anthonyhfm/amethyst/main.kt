package dev.anthonyhfm.amethyst

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.anthonyhfm.amethyst.core.engine.echo.Echo
import dev.anthonyhfm.amethyst.desktop.DiscordRPCManager
import dev.anthonyhfm.amethyst.desktop.utility.rememberTitleBarStyle
import dev.anthonyhfm.amethyst.settings.data.AudioSettings
import dev.anthonyhfm.amethyst.start.StartWindow
import dev.anthonyhfm.amethyst.ui.theme.AmethystTheme
import dev.anthonyhfm.amethyst.workspace.WorkspaceWindow
import dev.anthonyhfm.amethyst.workspace.utils.WorkspaceProjectOpenHelper
import dev.anthonyhfm.amethyst.workspace.utils.WorkspaceProjectOpenResult
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.window.NucleusDecoratedWindowTheme
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun main(args: Array<String>) {
    // The Tao backend turns macOS trackpad gestures into Compose Pan events
    // (PanStart/PanMove/PanEnd). The timeline, piano roll and waveform editor
    // handle wheel input through PointerEventType.Scroll, so keep the AWT-style
    // Scroll stream until those handlers understand Pan. Must be set before the
    // Tao scene host classes load — they read the flag once.
    System.setProperty("nucleus.tao.trackpadPanEvents", "false")

    initializeSentry()

    nucleusApplication(args = args, backend = NucleusBackend.Tao) {
        FileKit.init(appId = "Amethyst")

        onDeepLink { uri ->
            if (uri.scheme.equals("amethyst", ignoreCase = true)) {
                println("Received amethyst deep link: $uri")
            }
        }

        var showEditor: Boolean by remember { mutableStateOf(false) }

        // Tao must own the macOS main thread before optional services start.
        // Audio device setup is synchronous, so keep it off Tao's event loop.
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                Echo.setPreferredBufferFrames(AudioSettings.renderBufferFrames.value)
                Echo.setPreferredOutputDevice(
                    AudioSettings.outputDevice.value.takeUnless {
                        it == AudioSettings.SystemDefaultOutputDevice
                    }
                )
                Echo.setExclusiveMode(AudioSettings.exclusiveMode?.value == true)
                Echo.initialize()
            }
        }

        LaunchedEffect(Unit) {
            DiscordRPCManager.initialize()
        }

        LaunchedEffect(Unit) {
            if (args.isNotEmpty()) {
                val file = File(args[0])

                if (file.exists() && file.isFile) {
                    val result = WorkspaceProjectOpenHelper.openProject(
                        PlatformFile(file)
                    )

                    if (result is WorkspaceProjectOpenResult.Success) {
                        showEditor = true
                    }
                }
            }
        }

        AmethystTheme {
            NucleusDecoratedWindowTheme(
                isDark = true,
                titleBarStyle = rememberTitleBarStyle()
            ) {
                if (!showEditor) {
                    StartWindow(
                        onOpenEditor = {
                            showEditor = true
                        }
                    )
                } else {
                    WorkspaceWindow(
                        onQuit = ::exitApplication,
                        onClose = {
                            showEditor = false
                        }
                    )
                }
            }
        }
    }
}
