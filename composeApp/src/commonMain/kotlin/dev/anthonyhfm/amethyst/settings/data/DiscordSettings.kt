package dev.anthonyhfm.amethyst.settings.data

import dev.anthonyhfm.amethyst.desktop.DiscordRPCManager

object DiscordSettings : SettingsGroup("Discord") {
    val enableDiscordRPC: Setting.Toggle = toggle(
        key = "enableDiscordRPC",
        title = "Discord Rich Presence",
        default = true,
        onUpdate = { DiscordRPCManager.toggleRPC(it) },
    )

    val showCurrentProject: Setting.Toggle = toggle(
        key = "showCurrentProject",
        title = "Show Current Project",
        default = true,
        onUpdate = { DiscordRPCManager.forceUpdate() },
    )

    val showCurrentWorkspaceState: Setting.Toggle = toggle(
        key = "showCurrentWorkspaceState",
        title = "Show Current Workspace State",
        default = true,
        onUpdate = { DiscordRPCManager.forceUpdate() },
    )
}
