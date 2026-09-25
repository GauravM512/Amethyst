package dev.anthonyhfm.amethyst.home.ui.views

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.hub.data.HubApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val hubImageCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

internal fun hubImageUrl(path: String?): String? = when {
    path.isNullOrBlank() -> null
    path.startsWith("https://") -> path
    path.startsWith("/api/projects/") -> HubApiClient.DEFAULT_BASE_URL + path.removePrefix("/api")
    path.startsWith("/") -> HubApiClient.DEFAULT_BASE_URL + path
    else -> HubApiClient.DEFAULT_BASE_URL + "/" + path
}

@Composable
internal fun HubArtwork(
    url: String?,
    modifier: Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    icon: ImageVector = Icons.Default.MusicNote,
    contentDescription: String? = null,
) {
    val resolved = hubImageUrl(url)
    val bitmap by produceState<Bitmap?>(resolved?.let(hubImageCache::get), resolved) {
        value = if (resolved == null) null else hubImageCache.get(resolved) ?: withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(resolved).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7_000
                    readTimeout = 10_000
                }
                try {
                    connection.inputStream.use { input ->
                        BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.RGB_565
                        })
                    }?.also { hubImageCache.put(resolved, it) }
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }
    }
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap!!.asImageBitmap(), contentDescription, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.size(100.dp).offset(x = (-32).dp, y = (-28).dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = .11f)))
            Box(Modifier.size(70.dp).offset(x = 35.dp, y = 34.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.tertiary.copy(alpha = .13f)))
            Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.primary.copy(alpha = .72f), modifier = Modifier.size(40.dp))
        }
    }
}
