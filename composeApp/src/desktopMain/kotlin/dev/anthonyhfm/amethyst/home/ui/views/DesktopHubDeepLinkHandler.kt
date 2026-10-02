package dev.anthonyhfm.amethyst.home.ui.views

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.common_ok
import amethyst.composeapp.generated.resources.home_hub_detail_download_error
import amethyst.composeapp.generated.resources.home_hub_detail_download_open
import amethyst.composeapp.generated.resources.home_hub_detail_downloading
import amethyst.composeapp.generated.resources.home_hub_detail_loading
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.home.account.DesktopHubAccount
import dev.anthonyhfm.amethyst.hub.data.HubProjectDeepLink
import dev.anthonyhfm.amethyst.settings.AppLocaleProvider
import dev.anthonyhfm.amethyst.settings.data.HubSettings
import dev.anthonyhfm.amethyst.ui.components.primitives.Button
import dev.anthonyhfm.amethyst.ui.components.primitives.Spinner
import dev.anthonyhfm.amethyst.ui.theme.background
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.foreground
import dev.anthonyhfm.amethyst.workspace.utils.WorkspaceProjectOpenHelper
import dev.anthonyhfm.amethyst.workspace.utils.WorkspaceProjectOpenResult
import dev.nucleusframework.application.DecoratedDialog
import dev.nucleusframework.window.DialogTitleBar
import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ReceiveChannel
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DesktopHubDeepLinkHandler(
    links: ReceiveChannel<HubProjectDeepLink>,
    beforeOpenWorkspace: suspend () -> Boolean,
    onOpenWorkspace: () -> Unit,
) {
    val repository = remember { DesktopHubAccount.get().repository }
    val beforeOpen by rememberUpdatedState(newValue = beforeOpenWorkspace)
    val onOpened by rememberUpdatedState(newValue = onOpenWorkspace)
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(key1 = links, key2 = repository) {
        for (link in links) {
            var downloaded: File? = null
            error = null

            try {
                status = getString(resource = Res.string.home_hub_detail_loading)
                val project = link.resolve(repository = repository)
                val allowed = HubSettings.ignoreCompatibility.value ||
                    project.projectType.name == "amethyst" ||
                    project.compatibility.name == "compatible" ||
                    project.overrideDownloadUrl != null

                check(allowed) {
                    "This project is not marked as compatible with Amethyst."
                }

                check(DesktopHubDownload.canImport(project = project, repository = repository)) {
                    getString(resource = Res.string.home_hub_detail_download_error)
                }

                val downloading = getString(resource = Res.string.home_hub_detail_downloading)
                status = "$downloading ${project.title}"
                val file = DesktopHubDownload.download(
                    project = project,
                    repository = repository,
                    onProgress = { progress ->
                        status = "$downloading ${project.title} ${(progress * 100).toInt()}%"
                    },
                )
                downloaded = file
                status = null

                if (!beforeOpen()) {
                    continue
                }

                status = "${getString(resource = Res.string.home_hub_detail_loading)} ${project.title}"

                when (val result = WorkspaceProjectOpenHelper.openProject(file = PlatformFile(file = file))) {
                    is WorkspaceProjectOpenResult.Success -> {
                        DesktopHubDownload.completeImport(file = file)
                        downloaded = null
                        onOpened()
                    }

                    is WorkspaceProjectOpenResult.Failure -> {
                        error = result.message
                    }

                    WorkspaceProjectOpenResult.Cancelled -> Unit
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message
                    ?: getString(resource = Res.string.home_hub_detail_download_error)
            } finally {
                downloaded?.let { file ->
                    DesktopHubDownload.discardImport(file = file)
                }
                status = null
            }
        }
    }

    if (status != null || error != null) {
        DecoratedDialog(
            onCloseRequest = {
                if (status == null) {
                    error = null
                }
            },
            title = "Amethyst",
            state = rememberDialogState(
                width = 420.dp,
                height = 220.dp,
                position = WindowPosition.Aligned(alignment = Alignment.Center),
            ),
            resizable = false,
        ) {
            AppLocaleProvider {
                DialogTitleBar {
                    Text(
                        text = stringResource(resource = Res.string.home_hub_detail_download_open),
                        color = Theme[colors][foreground],
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Theme[colors][background])
                        .padding(all = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(space = 16.dp),
                ) {
                    if (status != null) {
                        Spinner()
                    }

                    Text(
                        text = status ?: error.orEmpty(),
                        color = Theme[colors][foreground],
                    )

                    if (status == null) {
                        Button(
                            onClick = {
                                error = null
                            },
                        ) {
                            Text(text = stringResource(resource = Res.string.common_ok))
                        }
                    }
                }
            }
        }
    }
}
