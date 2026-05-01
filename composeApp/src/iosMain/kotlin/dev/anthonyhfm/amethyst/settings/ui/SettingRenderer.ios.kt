package dev.anthonyhfm.amethyst.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.composeunstyled.Text
import dev.anthonyhfm.amethyst.settings.data.Setting
import dev.anthonyhfm.amethyst.settings.ui.components.SettingsItem
import dev.anthonyhfm.amethyst.ui.components.primitives.Slider
import dev.anthonyhfm.amethyst.ui.components.primitives.Switch
import dev.anthonyhfm.amethyst.ui.components.primitives.Tabs
import dev.anthonyhfm.amethyst.ui.components.primitives.TabsList
import dev.anthonyhfm.amethyst.ui.components.primitives.TabsTrigger

/**
 * iOS renderer for settings. Currently uses the shared Compose implementation.
 * Replace individual branches with SwiftUI interop when native iOS controls are desired.
 */
@Composable
actual fun SettingRenderer(setting: Setting<*>) {
    @Suppress("UNCHECKED_CAST")
    when (setting) {
        is Setting.Toggle -> ToggleSettingItem(setting)
        is Setting.Select<*> -> SelectSettingItem(setting as Setting.Select<Any>)
        is Setting.Slider -> SliderSettingItem(setting)
        is Setting.TextField -> TextFieldSettingItem(setting)
    }
}

@Composable
private fun ToggleSettingItem(setting: Setting.Toggle) {
    val checked by setting.flow.collectAsState()
    SettingsItem(title = setting.key) {
        Switch(
            checked = checked,
            onCheckedChange = { setting.update(it) },
        )
    }
}

@Composable
private fun <T : Any> SelectSettingItem(setting: Setting.Select<T>) {
    val selected by setting.flow.collectAsState()
    SettingsItem(title = setting.key) {
        Tabs(
            selectedTab = setting.label(selected),
            tabs = setting.options.map { setting.label(it) },
        ) {
            TabsList {
                setting.options.forEach { option ->
                    TabsTrigger(
                        key = setting.label(option),
                        selected = selected == option,
                        onSelected = { setting.update(option) },
                    ) {
                        Text(setting.label(option))
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderSettingItem(setting: Setting.Slider) {
    val value by setting.flow.collectAsState()
    SettingsItem(title = setting.key) {
        Slider(
            value = value,
            valueRange = setting.range,
            onValueChange = { setting.update(it) },
        )
    }
}

@Composable
private fun TextFieldSettingItem(setting: Setting.TextField) {
    val value by setting.flow.collectAsState()
    SettingsItem(title = setting.key) {
        Text(value)
    }
}
