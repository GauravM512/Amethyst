package dev.anthonyhfm.amethyst.home.ui.views

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.home_arcade_coming_description
import amethyst.composeapp.generated.resources.home_arcade_coming_title
import amethyst.composeapp.generated.resources.home_arcade_explore_hub
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Gamepad2
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.composeunstyled.Icon
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.ui.components.primitives.Button
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonVariant
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.foreground
import dev.anthonyhfm.amethyst.ui.theme.h2
import dev.anthonyhfm.amethyst.ui.theme.mutedForeground
import dev.anthonyhfm.amethyst.ui.theme.p
import dev.anthonyhfm.amethyst.ui.theme.primary
import dev.anthonyhfm.amethyst.ui.theme.primaryForeground
import dev.anthonyhfm.amethyst.ui.theme.typography
import org.jetbrains.compose.resources.stringResource

@Composable
fun ArcadeView(
    onExploreHub: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Lucide.Gamepad2,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp),
                tint = Theme[colors][primary]
            )

            Text(
                text = stringResource(Res.string.home_arcade_coming_title),
                style = Theme[typography][h2].copy(
                    color = Theme[colors][foreground],
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(Res.string.home_arcade_coming_description),
                style = Theme[typography][p].copy(
                    color = Theme[colors][mutedForeground],
                    textAlign = TextAlign.Center
                ),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onExploreHub,
                modifier = Modifier
                    .padding(top = 8.dp),
                variant = ButtonVariant.Default
            ) {
                Icon(
                    imageVector = Lucide.Globe,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp),
                    tint = Theme[colors][primaryForeground]
                )

                Text(
                    text = stringResource(Res.string.home_arcade_explore_hub)
                )
            }
        }
    }
}
