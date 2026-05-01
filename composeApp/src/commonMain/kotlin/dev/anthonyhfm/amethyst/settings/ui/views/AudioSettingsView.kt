package dev.anthonyhfm.amethyst.settings.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.settings.data.AudioSettings
import dev.anthonyhfm.amethyst.settings.ui.components.SettingsCategory
import dev.anthonyhfm.amethyst.settings.ui.components.SettingsItem
import dev.anthonyhfm.amethyst.ui.components.primitives.Slider
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.foreground
import dev.anthonyhfm.amethyst.ui.theme.small
import dev.anthonyhfm.amethyst.ui.theme.typography
import kotlin.math.roundToInt

@Composable
fun AudioSettingsView() {
    val volume by AudioSettings.masterVolume.flow.collectAsState()

    SettingsCategory(
        title = AudioSettings.title,
    ) {
        SettingsItem("Master Volume") {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${(volume * 100f).roundToInt()}%",
                    style = Theme[typography][small].copy(color = Theme[colors][foreground]),
                )

                Slider(
                    value = volume,
                    onValueChange = { AudioSettings.masterVolume.update(it) },
                    modifier = Modifier.width(220.dp),
                )
            }
        }
    }
}
