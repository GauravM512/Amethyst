package dev.anthonyhfm.amethyst.workspace.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import dev.anthonyhfm.amethyst.settings.AppLocaleProvider
import dev.anthonyhfm.amethyst.settings.IosLocalizationBridge
import dev.anthonyhfm.amethyst.ui.components.primitives.ScaleToFit
import dev.anthonyhfm.amethyst.ui.launchpad.viewport.ViewportMidiFighter64
import dev.anthonyhfm.amethyst.ui.theme.ComposeAmethystTheme
import dev.anthonyhfm.amethyst.workspace.ViewportRepository
import dev.anthonyhfm.amethyst.workspace.data.SavableWorkspaceData.SavableViewportLaunchpad.MidiFighter64.MidiFighter64Style

data class IosDeviceStyleOption(
    val id: String,
    val name: String,
)

private fun midiFighterForStyle(uuid: String): ViewportMidiFighter64? =
    ViewportRepository.devices.value.firstOrNull { it.selectionUUID == uuid } as? ViewportMidiFighter64

fun iosDeviceStyleOptions(uuid: String): List<IosDeviceStyleOption> {
    if (midiFighterForStyle(uuid = uuid) == null) {
        return emptyList()
    }

    return MidiFighter64Style.entries.map { style ->
        IosDeviceStyleOption(
            id = style.name,
            name = if (style == MidiFighter64Style.Black) {
                IosLocalizationBridge.string(
                    key = "ui_launchpad_midifighter_black_variant",
                    fallback = "Black Variant",
                )
            } else {
                IosLocalizationBridge.string(
                    key = "ui_launchpad_midifighter_white_variant",
                    fallback = "White Variant",
                )
            },
        )
    }
}

fun iosSelectedDeviceStyle(uuid: String): String? = midiFighterForStyle(uuid = uuid)?.style?.name

fun iosSelectDeviceStyle(uuid: String, styleId: String): Boolean {
    val device = midiFighterForStyle(uuid = uuid) ?: return false
    val style = MidiFighter64Style.entries.firstOrNull { it.name == styleId } ?: return false
    device.selectStyle(style = style)
    return true
}

@OptIn(ExperimentalComposeUiApi::class)
fun iosDeviceStylePreviewViewController(styleId: String, darkMode: Boolean) = ComposeUIViewController(
    configure = {
        opaque = false
    },
) {
    val preview = remember(styleId) {
        ViewportMidiFighter64(
            interactive = false,
            initialStyle = MidiFighter64Style.entries.firstOrNull { it.name == styleId }
                ?: MidiFighter64Style.Black,
        )
    }

    DisposableEffect(preview) {
        onDispose { preview.close() }
    }

    AppLocaleProvider {
        ComposeAmethystTheme(darkMode = darkMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                ScaleToFit {
                    preview.Content()
                }
            }
        }
    }
}
