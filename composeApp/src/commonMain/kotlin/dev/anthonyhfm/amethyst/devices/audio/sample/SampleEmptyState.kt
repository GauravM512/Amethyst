package dev.anthonyhfm.amethyst.devices.audio.sample

import amethyst.composeapp.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.twotone.AudioFile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Icon
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.ui.components.primitives.Button
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonVariant
import dev.anthonyhfm.amethyst.ui.components.primitives.Empty
import dev.anthonyhfm.amethyst.ui.components.primitives.EmptyActions
import dev.anthonyhfm.amethyst.ui.components.primitives.EmptyIcon
import dev.anthonyhfm.amethyst.ui.components.primitives.EmptyTitle
import dev.anthonyhfm.amethyst.ui.components.primitives.Spinner
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.mutedForeground
import dev.anthonyhfm.amethyst.ui.theme.mutedText
import dev.anthonyhfm.amethyst.ui.theme.secondaryForeground
import dev.anthonyhfm.amethyst.ui.theme.typography
import org.jetbrains.compose.resources.stringResource

@Composable
fun SampleEmptyState(
    onOpenSample: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Empty(
        modifier = modifier
            .fillMaxSize()
    ) {
        EmptyIcon(imageVector = Icons.TwoTone.AudioFile)
        EmptyTitle(text = stringResource(resource = Res.string.device_sample_empty))

        Spacer(
            modifier = Modifier
                .weight(weight = 1f)
        )

        EmptyActions {
            Button(
                onClick = onOpenSample,
                enabled = enabled,
                variant = ButtonVariant.Secondary
            ) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = null,
                    tint = Theme[colors][secondaryForeground]
                )

                Text(text = stringResource(resource = Res.string.device_sample_open))
            }
        }
    }
}

@Composable
fun SampleLoadingState(
    fileName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(all = 24.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterVertically
        )
    ) {
        Spinner(
            size = 32.dp,
            modifier = Modifier
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                }
        )

        EmptyTitle(text = stringResource(resource = Res.string.device_sample_loading))

        Text(
            text = fileName,
            style = Theme[typography][mutedText],
            color = Theme[colors][mutedForeground],
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
