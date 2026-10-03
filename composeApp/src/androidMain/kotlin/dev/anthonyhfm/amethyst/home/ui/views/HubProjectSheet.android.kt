package dev.anthonyhfm.amethyst.home.ui.views

import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import dev.anthonyhfm.amethyst.home.account.AndroidHubAccount
import dev.anthonyhfm.amethyst.hub.data.HubProject
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.net.URI
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HubProjectSheet(
    account: AndroidHubAccount,
    username: String,
    slug: String,
    onClose: () -> Unit,
    onSignIn: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onDownloadedFile: (PlatformFile) -> Unit,
) {
    val context = LocalContext.current
    val contentHeight = (LocalConfiguration.current.screenHeightDp.dp - 112.dp).coerceAtLeast(280.dp)
    val scope = rememberCoroutineScope()
    var project by remember(username, slug) { mutableStateOf<HubProject?>(null) }
    var loading by remember(username, slug) { mutableStateOf(true) }
    var retry by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var likeBusy by remember { mutableStateOf(false) }
    var downloadBusy by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    val animatedProgress by animateFloatAsState(
        targetValue = downloadProgress ?: 0f,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "Hub download progress",
    )
    val shareUrl = remember(username, slug) { "https://projects.launchpadders.com/@$username/$slug" }
    val likeError = stringResource(Res.string.home_hub_detail_like_error)
    val downloadError = stringResource(Res.string.home_hub_detail_download_error)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (actionError != null) AlertDialog(
        onDismissRequest = { actionError = null },
        title = { Text(stringResource(Res.string.home_hub_title)) },
        text = { Text(actionError.orEmpty()) },
        confirmButton = { TextButton(onClick = { actionError = null }) { Text(stringResource(Res.string.home_hub_dismiss)) } },
    )

    LaunchedEffect(username, slug, retry, account.sessionRevision) {
        loading = true
        error = false
        try { project = account.repository.getPublishedProject.execute(username, slug) }
        catch (_: Exception) { error = true }
        finally { loading = false }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        sheetMaxWidth = 720.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        // A fixed content height keeps the sheet anchors stable while its offset changes.
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.home_hub_dismiss))
                    }
                },
                actions = {
                        IconButton(onClick = {
                            val current = project ?: return@IconButton
                            if (!account.repository.client.isAuthenticated) { onSignIn(); return@IconButton }
                            scope.launch {
                                likeBusy = true
                                try {
                                    val updated = account.repository.toggleProjectLike.execute(current.id)
                                    project = current.copy(isLiked = updated.liked, likesCount = updated.likesCount)
                                } catch (_: Exception) { actionError = likeError }
                                finally { likeBusy = false }
                            }
                        }, enabled = project != null && !likeBusy) {
                            Icon(if (project?.isLiked == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(if (project?.isLiked == true) Res.string.home_hub_detail_unlike else Res.string.home_hub_detail_like))
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareUrl) }
                            runCatching { context.startActivity(Intent.createChooser(intent, null)) }
                                .onFailure { actionError = downloadError }
                        }) { Icon(Icons.Default.Share, contentDescription = stringResource(Res.string.home_hub_detail_share)) }
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().height(contentHeight), horizontalAlignment = Alignment.CenterHorizontally) {

                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    error || project == null -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(stringResource(Res.string.home_hub_detail_project_error), style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { retry++ }) { Text(stringResource(Res.string.home_hub_retry)) }
                    }
                    else -> {
                        val current = project!!
                        val description = remember(current.description) { HubProjectDescriptionAndroid(current.description) }
                        val externalUrl = HubProjectDownloader.externalDownloadUrl(project = current)
                        val internalSource = current.overrideDownloadUrl ?: current.downloadUrl ?: current.packageName?.let { "/projects/${current.id}/download" }
                        val youtubeUrl = remember(current.youtubeUrl) { validYoutubeUrl(current.youtubeUrl) }
                        val canImport = HubProjectDownloader.canImport(
                            repository = account.repository,
                            project = current,
                            externalUrl = externalUrl,
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
                        ) {
                            item {
                                BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                                    val heroWidth = minOf(maxWidth - 40.dp, 600.dp)
                                    HubArtwork(
                                        current.thumbnailUrl,
                                        Modifier.width(heroWidth).aspectRatio(16f / 9f),
                                        MaterialTheme.shapes.extraLarge,
                                        contentDescription = current.title,
                                    )
                                }
                            }
                            item {
                                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        HubArtwork(current.artist.avatarUrl, Modifier.size(44.dp), CircleShape, Icons.Default.Person)
                                        Column(Modifier.weight(1f)) {
                                            Text(current.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                            Text(
                                                current.artist.displayName.ifBlank { "@${current.artist.username}" },
                                                modifier = Modifier.clickable { onOpenArtist(current.artist.username) },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (externalUrl != null || internalSource != null) {
                                            val progress = downloadProgress
                                            Box(Modifier.fillMaxWidth().height(52.dp).clip(CircleShape)) {
                                                Box(
                                                    Modifier.fillMaxSize().background(
                                                        if (downloadBusy) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                        else MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                                if (downloadBusy && progress != null) {
                                                    Box(
                                                        Modifier.fillMaxHeight().fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    )
                                                }
                                                Button(
                                                    onClick = {
                                                        if (!canImport) {
                                                            openHubLink(context, externalUrl ?: account.repository.client.resolveUrl(internalSource!!)) { actionError = downloadError }
                                                        } else scope.launch {
                                                            downloadBusy = true
                                                            downloadProgress = null
                                                            try {
                                                                val file = HubProjectDownloader.download(account.repository, current, externalUrl) { downloadProgress = it }
                                                                onDownloadedFile(file)
                                                            } catch (failure: Exception) {
                                                                android.util.Log.e("HubProjectDownload", "Could not import Hub project ${current.id}", failure)
                                                                actionError = downloadError
                                                            }
                                                            finally { downloadBusy = false }
                                                        }
                                                    },
                                                    enabled = !downloadBusy,
                                                    modifier = Modifier.fillMaxSize(),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color.Transparent,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                                        disabledContainerColor = Color.Transparent,
                                                        disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                                                    ),
                                                ) {
                                                    Icon(
                                                        if (!canImport && externalUrl != null) Icons.Default.OpenInNew else Icons.Default.Download,
                                                        contentDescription = null,
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(
                                                        when {
                                                            downloadBusy && progress != null -> "${stringResource(Res.string.home_hub_detail_downloading)} (${(progress * 100).toInt()}%)"
                                                            downloadBusy -> stringResource(Res.string.home_hub_detail_downloading)
                                                            canImport -> stringResource(Res.string.home_hub_detail_download_open)
                                                            else -> stringResource(Res.string.home_hub_detail_download)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        if (youtubeUrl != null) FilledTonalButton(
                                            onClick = { openHubLink(context, youtubeUrl) { actionError = downloadError } },
                                            modifier = Modifier.fillMaxWidth().height(52.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                            Spacer(Modifier.width(10.dp))
                                            Text(stringResource(Res.string.home_hub_detail_watch_youtube))
                                        }
                                    }

                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        HubProjectMetric(Icons.Default.Visibility, current.views, stringResource(Res.string.home_hub_detail_views), Modifier.weight(1f))
                                        HubProjectMetric(Icons.Default.Download, current.downloadsCount, stringResource(Res.string.home_hub_detail_downloads), Modifier.weight(1f))
                                        HubProjectMetric(Icons.Default.FavoriteBorder, current.likesCount, stringResource(Res.string.home_hub_detail_likes), Modifier.weight(1f))
                                    }
                                    HorizontalDivider()
                                    if (description.text.isNotBlank()) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(stringResource(Res.string.home_hub_detail_about), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                        Text(description.text, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(stringResource(Res.string.home_hub_detail_details), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                        HubProjectAttribute(stringResource(Res.string.home_hub_detail_format), current.projectType.name.replaceFirstChar(Char::uppercase))
                                        HubProjectAttribute(stringResource(Res.string.home_hub_detail_compatibility), current.compatibility.name.replaceFirstChar(Char::uppercase))
                                        HubProjectDifficulty(stringResource(Res.string.home_hub_detail_difficulty), current.difficulty)
                                        val published = current.publishedAt ?: current.created
                                        if (published > 0) HubProjectAttribute(stringResource(Res.string.home_hub_detail_published), DateFormat.getDateInstance(DateFormat.LONG).format(Date(published * 1000)))
                                        if (current.overrideDownloadUrl != null) {
                                            val overrideName = current.overrideName ?: "Amethyst .ame"
                                            val overrideValue = current.overrideSize?.let { "$overrideName (${Formatter.formatFileSize(context, it)})" } ?: overrideName
                                            HubProjectAttribute(stringResource(Res.string.home_hub_detail_amethyst_file), overrideValue)
                                        }
                                        if (current.packageName != null) HubProjectAttribute(stringResource(Res.string.home_hub_detail_package_file), current.packageName)
                                        else externalUrl?.let { url ->
                                            Uri.parse(url).host?.let { host ->
                                                HubProjectAttribute(stringResource(Res.string.home_hub_detail_download_source), host)
                                            }
                                        }
                                        current.packageSize?.let { HubProjectAttribute(stringResource(Res.string.home_hub_detail_size), Formatter.formatFileSize(context, it)) }
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun HubProjectMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: Long, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(java.text.NumberFormat.getIntegerInstance().format(value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HubProjectAttribute(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HubProjectDifficulty(label: String, difficulty: Int) {
    val rating = difficulty.coerceIn(0, 10)
    val ratingText = String.format(Locale.getDefault(), "%.1f / 5.0", rating / 2.0)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.weight(1.4f).semantics(mergeDescendants = true) { contentDescription = "$label $ratingText" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            repeat(5) { index ->
                val icon = when {
                    rating >= (index + 1) * 2 -> Icons.Default.Star
                    rating == index * 2 + 1 -> Icons.Default.StarHalf
                    else -> Icons.Default.StarBorder
                }
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFB803))
            }
            Spacer(Modifier.width(4.dp))
            Text(ratingText, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

private class HubProjectDescriptionAndroid(raw: String) {
    private val start = raw.indexOf("<!--")
    private val end = if (start >= 0) raw.indexOf("-->", start + 4) else -1
    private val metadata = if (end >= 0) raw.substring(start + 4, end).trim()
        .takeIf { it.startsWith("glacier-meta:") }
        ?.removePrefix("glacier-meta:")?.trim() else null

    val text: String = if (metadata != null) raw.removeRange(start, end + 3).trim() else raw.trim()
}

private fun validYoutubeUrl(raw: String?): String? {
    val value = raw?.trim()?.takeIf(String::isNotBlank) ?: return null
    if (Regex("[A-Za-z0-9_-]{11}").matches(value)) return "https://www.youtube.com/watch?v=$value"
    val uri = runCatching { URI(value) }.getOrNull() ?: return null
    return value.takeIf { uri.scheme == "https" && uri.host?.lowercase() in setOf("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be", "www.youtu.be") }
}

private fun openHubLink(context: android.content.Context, value: String, onError: () -> Unit) {
    val uri = runCatching { Uri.parse(value) }.getOrNull()
    if (uri == null || uri.scheme != "https") { onError(); return }
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        .onFailure { onError() }
}
