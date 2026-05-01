package dev.anthonyhfm.amethyst.settings.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.anthonyhfm.amethyst.desktop.DiscordRPCManager
import dev.anthonyhfm.amethyst.settings.data.DiscordSettings
import dev.anthonyhfm.amethyst.settings.ui.components.SettingsCategory
import dev.anthonyhfm.amethyst.settings.ui.components.SettingsItem
import dev.anthonyhfm.amethyst.ui.components.primitives.Switch

@Composable
fun DiscordSettingsView() {
    val discordRPC by DiscordSettings.enableDiscordRPC.flow.collectAsState()
    val showCurrentProject by DiscordSettings.showCurrentProject.flow.collectAsState()
    val showCurrentWorkspaceState by DiscordSettings.showCurrentWorkspaceState.flow.collectAsState()

    SettingsCategory(
        title = DiscordSettings.title,
    ) {
        SettingsItem(
            title = "Discord Rich Presence",
        ) {
            Switch(
                checked = discordRPC,
                onCheckedChange = {
                    DiscordSettings.enableDiscordRPC.update(it)
                    DiscordRPCManager.toggleRPC(it)
                }
            )
        }

        SettingsItem(
            title = "Show Current Project",
        ) {
            Switch(
                checked = showCurrentProject,
                enabled = discordRPC,
                onCheckedChange = {
                    DiscordSettings.showCurrentProject.update(it)
                    DiscordRPCManager.forceUpdate()
                }
            )
        }

        SettingsItem(
            title = "Show Current Workspace State",
        ) {
            Switch(
                checked = showCurrentWorkspaceState,
                enabled = discordRPC,
                onCheckedChange = {
                    DiscordSettings.showCurrentWorkspaceState.update(it)
                    DiscordRPCManager.forceUpdate()
                }
            )
        }
    }
}
