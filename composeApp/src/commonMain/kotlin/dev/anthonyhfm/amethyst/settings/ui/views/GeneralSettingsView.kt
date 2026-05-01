package dev.anthonyhfm.amethyst.settings.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.composeunstyled.Text
import dev.anthonyhfm.amethyst.core.util.getDeviceCapabilities
import dev.anthonyhfm.amethyst.settings.data.GeneralSettings
import dev.anthonyhfm.amethyst.settings.ui.components.SettingsCategory
import dev.anthonyhfm.amethyst.settings.ui.components.SettingsItem
import dev.anthonyhfm.amethyst.ui.components.primitives.Tabs
import dev.anthonyhfm.amethyst.ui.components.primitives.TabsList
import dev.anthonyhfm.amethyst.ui.components.primitives.TabsTrigger

@Composable
fun GeneralSettingsView() {
    val caps = getDeviceCapabilities()

    val selectedFPS by GeneralSettings.performanceFPS.flow.collectAsState()
    val selectedGradientSmoothness by GeneralSettings.gradientSmoothness.flow.collectAsState()

    SettingsCategory(
        title = GeneralSettings.title,
    ) {
        SettingsItem(
            title = "Frames per Second (FPS)",
        ) {
            val fpsChoices = if (caps.showFPSSettings) listOf(60, 90, 120) else listOf(caps.initialFPS)
            Tabs(
                selectedTab = selectedFPS.toString(),
                tabs = fpsChoices.map(Int::toString),
            ) {
                TabsList {
                    fpsChoices.forEach { fps ->
                        TabsTrigger(
                            key = fps.toString(),
                            selected = selectedFPS == fps,
                            onSelected = { GeneralSettings.performanceFPS.update(fps) },
                        ) {
                            Text(fps.toString())
                        }
                    }
                }
            }
        }

        SettingsItem(
            title = "Gradient Smoothness",
        ) {
            val gradientChoices = GeneralSettings.gradientSmoothness.options
            Tabs(
                selectedTab = selectedGradientSmoothness.toString(),
                tabs = gradientChoices.map(Float::toString),
            ) {
                TabsList {
                    gradientChoices.forEach { value ->
                        TabsTrigger(
                            key = value.toString(),
                            selected = selectedGradientSmoothness == value,
                            onSelected = { GeneralSettings.gradientSmoothness.update(value) },
                        ) {
                            Text(GeneralSettings.gradientSmoothness.label(value))
                        }
                    }
                }
            }
        }
    }
}
