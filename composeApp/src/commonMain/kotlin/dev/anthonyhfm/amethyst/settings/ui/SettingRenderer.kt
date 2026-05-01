package dev.anthonyhfm.amethyst.settings.ui

import androidx.compose.runtime.Composable
import dev.anthonyhfm.amethyst.settings.data.Setting

/**
 * Platform-specific renderer for a single [Setting]. Each platform provides its own
 * `actual` implementation so that iOS can use SwiftUI controls, Android/Desktop can
 * use Compose Material controls, etc.
 */
@Composable
expect fun SettingRenderer(setting: Setting<*>)
