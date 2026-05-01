package dev.anthonyhfm.amethyst.settings.data

import com.russhwolf.settings.Settings
import dev.anthonyhfm.amethyst.core.util.Platform
import dev.anthonyhfm.amethyst.core.util.platform
import dev.anthonyhfm.amethyst.ui.components.primitives.TypographyLead

object SettingsRepository {
    val platformSettings = Settings()

    val settingsGroups: MutableList<SettingsGroup> = mutableListOf()

    init {
        settingsGroups.add(GeneralSettings)
        settingsGroups.add(AudioSettings)

        if (platform is Platform.Desktop) {
            settingsGroups.add(DiscordSettings)
        }

        settingsGroups.add(ExperimentalSettings)
    }
}