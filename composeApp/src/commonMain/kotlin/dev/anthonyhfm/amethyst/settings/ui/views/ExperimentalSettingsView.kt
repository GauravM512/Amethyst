package dev.anthonyhfm.amethyst.settings.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.anthonyhfm.amethyst.settings.data.ExperimentalSettings
import dev.anthonyhfm.amethyst.settings.ui.components.SettingsCategory
import dev.anthonyhfm.amethyst.settings.ui.components.SettingsItem
import dev.anthonyhfm.amethyst.ui.components.primitives.Switch

@Composable
fun ExperimentalSettingsView() {
    val abletonPush2Support by ExperimentalSettings.abletonPush2Support.flow.collectAsState()
    val apolloConversionSupport by ExperimentalSettings.apolloConversionSupport.flow.collectAsState()
    val enableExtension by ExperimentalSettings.extensions.flow.collectAsState()

    SettingsCategory(
        title = ExperimentalSettings.title,
    ) {
        SettingsItem(
            title = "Ableton Push 2 Support",
        ) {
            Switch(
                checked = abletonPush2Support,
                onCheckedChange = { ExperimentalSettings.abletonPush2Support.update(it) }
            )
        }

        SettingsItem(
            title = "Apollo Conversion Support",
        ) {
            Switch(
                checked = apolloConversionSupport,
                onCheckedChange = { ExperimentalSettings.apolloConversionSupport.update(it) }
            )
        }

        SettingsItem(
            title = "Amethyst Gems",
        ) {
            Switch(
                checked = enableExtension,
                onCheckedChange = { ExperimentalSettings.extensions.update(it) }
            )
        }
    }
}
