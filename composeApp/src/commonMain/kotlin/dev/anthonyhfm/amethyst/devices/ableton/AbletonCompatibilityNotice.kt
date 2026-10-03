package dev.anthonyhfm.amethyst.devices.ableton

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.device_ableton_compatibility_notice
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.foreground
import dev.anthonyhfm.amethyst.ui.theme.small
import dev.anthonyhfm.amethyst.ui.theme.typography
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AbletonCompatibilityNotice() {
    Text(
        text = stringResource(resource = Res.string.device_ableton_compatibility_notice),
        modifier = Modifier
            .padding(all = 8.dp),
        style = Theme[typography][small],
        color = Theme[colors][foreground],
        textAlign = TextAlign.Center,
    )
}
