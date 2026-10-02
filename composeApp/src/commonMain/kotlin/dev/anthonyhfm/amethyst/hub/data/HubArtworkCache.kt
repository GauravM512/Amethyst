package dev.anthonyhfm.amethyst.hub.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import dev.anthonyhfm.amethyst.workspace.help.decodeImageBytes
import kotlinx.coroutines.CancellationException

internal val hubArtworkCache = HubImageCache(
    decode = ::decodeImageBytes,
    sizeOf = { image -> image.width.toLong() * image.height * 4 },
)

internal fun hubImageUrl(path: String?): String? = when {
    path.isNullOrBlank() -> null
    path.startsWith(prefix = "https://") || path.startsWith(prefix = "http://") -> path
    path.startsWith(prefix = "/api/projects/") -> HubApiClient.DEFAULT_BASE_URL + path.removePrefix(prefix = "/api")
    else -> HubApiClient.DEFAULT_BASE_URL + "/" + path.trimStart('/')
}

internal suspend fun invalidateHubAvatar(previous: String?, updated: String?) {
    listOfNotNull(hubImageUrl(path = previous), hubImageUrl(path = updated))
        .distinct()
        .forEach { url -> hubArtworkCache.invalidate(url = url) }
}

@Composable
internal fun rememberHubImage(path: String?): ImageBitmap? {
    val url = hubImageUrl(path = path)
    val revision by hubArtworkCache.revision.collectAsState()

    return key(url, revision) {
        val image by produceState<ImageBitmap?>(initialValue = null, key1 = url) {
            value = if (url == null) {
                null
            } else {
                try {
                    hubArtworkCache.load(url = url)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
        }
        image
    }
}
