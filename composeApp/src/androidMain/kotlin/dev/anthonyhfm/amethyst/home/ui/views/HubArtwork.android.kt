package dev.anthonyhfm.amethyst.home.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.hub.data.rememberHubImage

@Composable
internal fun HubArtwork(
    url: String?,
    modifier: Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    icon: ImageVector = Icons.Default.MusicNote,
    contentDescription: String? = null,
) {
    val bitmap = rememberHubImage(path = url)

    Box(
        modifier = modifier
            .clip(shape = shape)
            .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size = 100.dp)
                    .offset(x = (-32).dp, y = (-28).dp)
                    .clip(shape = CircleShape)
                    .background(color = MaterialTheme.colorScheme.primary.copy(alpha = .11f))
            )

            Box(
                modifier = Modifier
                    .size(size = 70.dp)
                    .offset(x = 35.dp, y = 34.dp)
                    .clip(shape = MaterialTheme.shapes.large)
                    .background(color = MaterialTheme.colorScheme.tertiary.copy(alpha = .13f))
            )

            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = .72f),
                modifier = Modifier
                    .size(size = 40.dp),
            )
        }
    }
}
