package dev.anthonyhfm.amethyst.settings.data

object AudioSettings : SettingsGroup("Audio") {
    val masterVolume: Setting.Slider = slider(
        key = "masterVolume",
        title = "Master Volume",
        default = 1f,
        range = 0f..1f,
    )
}
