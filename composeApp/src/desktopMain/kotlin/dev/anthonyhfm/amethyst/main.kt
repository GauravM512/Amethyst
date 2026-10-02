package dev.anthonyhfm.amethyst

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.anthonyhfm.amethyst.core.engine.echo.Echo
import dev.anthonyhfm.amethyst.desktop.DiscordRPCManager
import dev.anthonyhfm.amethyst.desktop.utility.rememberTitleBarStyle
import dev.anthonyhfm.amethyst.home.ui.views.DesktopHubDeepLinkHandler
import dev.anthonyhfm.amethyst.hub.data.HubDeepLinks
import dev.anthonyhfm.amethyst.hub.data.HubProjectDeepLink
import dev.anthonyhfm.amethyst.settings.data.AudioSettings
import dev.anthonyhfm.amethyst.start.StartWindow
import dev.anthonyhfm.amethyst.ui.theme.AmethystTheme
import dev.anthonyhfm.amethyst.workspace.WorkspaceWindow
import dev.anthonyhfm.amethyst.workspace.utils.WorkspaceProjectOpenHelper
import dev.anthonyhfm.amethyst.workspace.utils.WorkspaceProjectOpenResult
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.window.NucleusDecoratedWindowTheme
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

fun main(args: Array<String>) {
    // The Tao backend turns macOS trackpad gestures into Compose Pan events
    // (PanStart/PanMove/PanEnd). The timeline, piano roll and waveform editor
    // handle wheel input through PointerEventType.Scroll, so keep the AWT-style
    // Scroll stream until those handlers understand Pan. Must be set before the
    // Tao scene host classes load — they read the flag once.
    System.setProperty("nucleus.tao.trackpadPanEvents", "false")

    initializeSentry()

    val projectLinks = Channel<HubProjectDeepLink>(capacity = Channel.UNLIMITED)

    nucleusApplication(args = args) {
        FileKit.init(appId = "Amethyst")

        onDeepLink { uri ->
            HubDeepLinks.parse(value = uri.toString())?.let { link ->
                projectLinks.trySend(element = link)
            }
        }

        var showEditor: Boolean by remember { mutableStateOf(false) }
        var externalCloseRequest by remember { mutableIntStateOf(0) }
        var pendingProjectCloseResponse by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }

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
                        externalCloseRequest = externalCloseRequest,
                        onExternalCloseConfirmed = {
                            showEditor = false
                            externalCloseRequest = 0
                            val projectResponse = pendingProjectCloseResponse
                            pendingProjectCloseResponse = null
                            projectResponse?.complete(value = true)
                        },
                        onExternalCloseCancelled = {
                            externalCloseRequest = 0
                            val projectResponse = pendingProjectCloseResponse
                            pendingProjectCloseResponse = null
                            projectResponse?.complete(value = false)
                        },
                        onClose = {
                            showEditor = false
                        }
                    )
                }

                DesktopHubDeepLinkHandler(
                    links = projectLinks,
                    beforeOpenWorkspace = {
                        if (showEditor) {
                            val response = CompletableDeferred<Boolean>()
                            pendingProjectCloseResponse = response
                            externalCloseRequest += 1
                            response.await()
                        } else {
                            true
                        }
                    },
                    onOpenWorkspace = {
                        showEditor = true
                    },
                )
            }
        }
    }
}
