package dev.anthonyhfm.amethyst.settings.data

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.settings_hub_group_title
import amethyst.composeapp.generated.resources.settings_hub_ignore_compatibility_title

object HubSettings : SettingsGroup("Hub", Res.string.settings_hub_group_title) {
    val ignoreCompatibility: Setting.Toggle = toggle(
        key = "hubIgnoreCompatibility",
        title = "Ignore compatibility",
        titleRes = Res.string.settings_hub_ignore_compatibility_title,
        default = false,
    )
}
