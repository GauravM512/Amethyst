package dev.anthonyhfm.amethyst.devices.effects.keyframes.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FrameControl(
    modifier: Modifier = Modifier,
    currentFrame: Int,
    isPlaying: Boolean,
    onPreviousFrame: () -> Unit,
    onNextFrame: () -> Unit,
    onPlayPause: () -> Unit
) {
    Card(
        modifier = modifier
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousFrame
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Frame"
                )
            }

            IconButton(
                onClick = onPlayPause
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Play"
                )
            }

            IconButton(
                onClick = onNextFrame
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Frame"
                )
            }
        }
    }
}
