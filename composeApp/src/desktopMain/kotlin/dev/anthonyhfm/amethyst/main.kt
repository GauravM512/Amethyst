package dev.anthonyhfm.amethyst

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.application
import dev.anthonyhfm.amethyst.core.engine.echo.AudioOutput
import dev.anthonyhfm.amethyst.desktop.DesktopPlatform
import dev.anthonyhfm.amethyst.desktop.DiscordRPCManager
import dev.anthonyhfm.amethyst.desktop.about.setupAboutHandler
import dev.anthonyhfm.amethyst.settings.setupPreferencesHandler
import dev.anthonyhfm.amethyst.start.EarlyAccessWindow
import dev.anthonyhfm.amethyst.settings.data.SettingsRepository
import dev.anthonyhfm.amethyst.start.StartWindow
import dev.anthonyhfm.amethyst.workspace.WorkspaceWindow
import io.github.vinceglb.filekit.FileKit
import kotlin.system.exitProcess

fun main() {
    initializeSentry()

    val platform = DesktopPlatform.get()

    if (platform == DesktopPlatform.MacOS) {
        System.setProperty("apple.awt.application.name", "Amethyst")
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")

        setupAboutHandler()
        setupPreferencesHandler()
    }

    AudioOutput // <- Initialize AudioOutput, no constructor call, just reference
    
    // Initialize Discord RPC manager (will connect if enabled in settings)
    DiscordRPCManager.initialize()

    application {
        FileKit.init(appId = "Amethyst")

        var showEditor: Boolean by remember { mutableStateOf(false) }
        var hasAcceptedEarlyAccess by remember {
            mutableStateOf(
                SettingsRepository.platformSettings.getBoolean("early_access_accepted", false)
            )
        }

        if (!hasAcceptedEarlyAccess) {
            EarlyAccessWindow(
                onAccept = {
                    SettingsRepository.platformSettings.putBoolean("early_access_accepted", true)
                    hasAcceptedEarlyAccess = true
                },
                onCancel = {
                    exitProcess(0)
                }
            )
        } else if (!showEditor) {
            StartWindow(
                onOpenEditor = {
                    println("[main ${System.currentTimeMillis()}] onOpenEditor -> showEditor=true")
                    showEditor = true
                }
            )
        } else {
            println("[main ${System.currentTimeMillis()}] rendering WorkspaceWindow")
            WorkspaceWindow(
                onClose = {
                    println("[main ${System.currentTimeMillis()}] WorkspaceWindow onClose -> showEditor=false")
                    showEditor = false
                }
            )
        }
    }
}

