package dev.anthonyhfm.amethyst.settings.data

object ExperimentalSettings : SettingsGroup("Experimental Features") {
    val abletonPush2Support: Setting.Toggle = toggle(
        key = "experimentalAbletonPush2Support",
        title = "Ableton Push 2",
        default = false,
    )

    val extensions: Setting.Toggle = toggle(
        key = "experimentalExtensions",
        title = "Amethyst Gems",
        default = false,
    )
}
