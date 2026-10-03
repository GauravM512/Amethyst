package dev.anthonyhfm.amethyst

import android.graphics.Color
import android.Manifest
import android.os.Bundle
import android.os.Build
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ProcessLifecycleOwner
import dev.anthonyhfm.amethyst.core.engine.echo.AndroidAudioLifecycleObserver
import dev.anthonyhfm.amethyst.core.engine.echo.Echo
import dev.anthonyhfm.amethyst.core.midi.AndroidMidiAccessProvider
import dev.anthonyhfm.amethyst.core.util.AndroidDeviceType
import dev.anthonyhfm.amethyst.core.util.MobileFileStorage
import dev.anthonyhfm.amethyst.home.data.HomeRepository
import dev.anthonyhfm.amethyst.home.account.AndroidHubAccount
import dev.anthonyhfm.amethyst.home.ui.views.HubProjectDownloader
import dev.anthonyhfm.amethyst.home.ui.views.LoadingScreenView
import dev.anthonyhfm.amethyst.core.loading.ProjectLoadingManager
import dev.anthonyhfm.amethyst.hub.data.HubDeepLinks
import dev.anthonyhfm.amethyst.hub.data.HubProjectDeepLink
import dev.anthonyhfm.amethyst.nativeengine.AndroidNativeContext
import dev.anthonyhfm.amethyst.settings.AppLocaleProvider
import dev.anthonyhfm.amethyst.settings.data.AudioSettings
import dev.anthonyhfm.amethyst.ui.theme.ComposeAmethystTheme
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import dev.anthonyhfm.amethyst.workspace.ui.components.ExitWorkspaceDialog
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.home_hub_detail_download_error
import amethyst.composeapp.generated.resources.home_hub_detail_downloading
import amethyst.composeapp.generated.resources.home_hub_dismiss
import amethyst.composeapp.generated.resources.home_hub_title
import amethyst.composeapp.generated.resources.home_projects_failed_open_recent_msg
import amethyst.composeapp.generated.resources.home_projects_opening_project_msg
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    private val deepLinkMutex = Mutex()
    private var externalWorkspaceOpenCount by mutableIntStateOf(0)
    private var deepLinkLoading by mutableStateOf(false)
    private var deepLinkError by mutableStateOf<String?>(null)
    private var pendingWorkspaceConfirmation by mutableStateOf<CompletableDeferred<Boolean>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashView = splashScreenViewProvider.view
            // Some Samsung splashscreen implementations expose no icon view here.
            val iconView = runCatching { splashScreenViewProvider.iconView }.getOrNull()
            val fadeOut = android.animation.ObjectAnimator.ofFloat(splashView, android.view.View.ALPHA, 1.0f, 0.0f)
            val animations = mutableListOf<android.animation.Animator>(fadeOut)
            if (iconView != null) {
                animations += android.animation.ObjectAnimator.ofFloat(iconView, android.view.View.SCALE_X, 1.0f, 1.75f)
                animations += android.animation.ObjectAnimator.ofFloat(iconView, android.view.View.SCALE_Y, 1.0f, 1.75f)
            }

            val exitAnimatorSet = android.animation.AnimatorSet().apply {
                playTogether(animations)
                duration = 500L
                interpolator = android.view.animation.DecelerateInterpolator(1.5f)
                startDelay = 100L
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        splashScreenViewProvider.remove()
                    }
                })
            }
            exitAnimatorSet.start()
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        AndroidDeviceType.initialize(context = applicationContext)

        initializeSentry()

        check(AndroidNativeContext.initialize(applicationContext)) {
            "Cannot initialize the native Android context"
        }

        FileKit.init(this)
        AndroidMidiAccessProvider.initialize(applicationContext)
        requestBluetoothMidiPermissionIfNeeded()

        Echo.setPreferredBufferFrames(AudioSettings.renderBufferFrames.value)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AndroidAudioLifecycleObserver)

        handleIntent(intent)

        setContent {
            val darkMode = true

            ApplySystemBarStyle(
                window = window,
                darkMode = darkMode,
            )

            ComposeAmethystTheme(
                darkMode = darkMode,
            ) {
                AppLocaleProvider {
                    App(externalWorkspaceOpenCount = externalWorkspaceOpenCount)
                    DeepLinkDialogs()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val action = intent.action
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_EDIT) {
            return
        }

        if (uri.scheme.equals(DEEP_LINK_SCHEME, ignoreCase = true)) {
            handleDeepLink(uri)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val inputStream = contentResolver.openInputStream(uri) ?: return@runCatching
                val bytes = inputStream.use { it.readBytes() }
                val filename = resolveFileName(uri) ?: "imported_project.ame"
                val persistentFile = MobileFileStorage.copyBytesToPersistentStorage(bytes, filename)
                val workspace = HomeRepository.loadWorkspaceData(persistentFile)
                HomeRepository.openWorkspace(workspace, rememberRecent = true)
            }
        }
    }

    private fun handleDeepLink(uri: Uri) {
        val link = HubDeepLinks.parse(value = uri.toString()) ?: return
        lifecycleScope.launch {
            deepLinkMutex.withLock {
                openHubProject(link = link)
                if (intent?.data == uri) {
                    intent.data = null
                }
            }
        }
    }

    private suspend fun openHubProject(link: HubProjectDeepLink) {
        if (WorkspaceRepository.hasUnsavedChanges()) {
            val confirmation = CompletableDeferred<Boolean>()
            pendingWorkspaceConfirmation = confirmation
            try {
                if (!confirmation.await()) {
                    return
                }
            } finally {
                pendingWorkspaceConfirmation = null
            }
        }

        var downloadFinished = false
        deepLinkError = null
        deepLinkLoading = true
        try {
            val downloadingText = getString(resource = Res.string.home_hub_detail_downloading)
            ProjectLoadingManager.startLoading(initialStatus = downloadingText)
            ProjectLoadingManager.setIndeterminate(statusText = downloadingText)

            val repository = AndroidHubAccount.get(context = applicationContext).repository
            val project = link.resolve(repository = repository)
            val externalUrl = HubProjectDownloader.externalDownloadUrl(project = project)
            require(
                HubProjectDownloader.canImport(
                    repository = repository,
                    project = project,
                    externalUrl = externalUrl,
                )
            ) { "Project source is not directly importable" }

            val file = HubProjectDownloader.download(
                repository = repository,
                project = project,
                externalUrl = externalUrl,
                onProgress = { progress ->
                    ProjectLoadingManager.updateProgress(
                        progress = progress,
                        statusText = downloadingText,
                        detailText = project.title,
                    )
                },
            )
            downloadFinished = true
            ProjectLoadingManager.startLoading(
                initialStatus = getString(resource = Res.string.home_projects_opening_project_msg),
            )
            val workspace = HomeRepository.loadWorkspaceData(file = file)
            HomeRepository.openWorkspace(workspace = workspace, rememberRecent = true)
            externalWorkspaceOpenCount += 1
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.e("HubDeepLink", "Could not open Hub project ${link.projectId}: $error", error)
            deepLinkError = getString(
                resource = if (downloadFinished) {
                    Res.string.home_projects_failed_open_recent_msg
                } else {
                    Res.string.home_hub_detail_download_error
                },
            )
        } finally {
            ProjectLoadingManager.finishLoading()
            deepLinkLoading = false
        }
    }

    @Composable
    private fun DeepLinkDialogs() {
        if (deepLinkLoading) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                ),
            ) {
                LoadingScreenView()
            }
        }

        pendingWorkspaceConfirmation?.let { confirmation ->
            ExitWorkspaceDialog(
                onSaveAndExit = {
                    lifecycleScope.launch {
                        if (HomeRepository.saveOpenMobileWorkspace()) {
                            confirmation.complete(value = true)
                        }
                    }
                },
                onDiscardAndExit = {
                    confirmation.complete(value = true)
                },
                onCancel = {
                    confirmation.complete(value = false)
                },
            )
        }

        deepLinkError?.let { error ->
            AlertDialog(
                onDismissRequest = { deepLinkError = null },
                title = {
                    Text(text = stringResource(resource = Res.string.home_hub_title))
                },
                text = {
                    Text(text = error)
                },
                confirmButton = {
                    TextButton(onClick = { deepLinkError = null }) {
                        Text(text = stringResource(resource = Res.string.home_hub_dismiss))
                    }
                },
            )
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }

    private fun requestBluetoothMidiPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), BLUETOOTH_MIDI_PERMISSION_REQUEST)
        }
    }

    private companion object {
        const val BLUETOOTH_MIDI_PERMISSION_REQUEST = 4101
        const val DEEP_LINK_SCHEME = "amethyst"
    }
}

@Composable
private fun ApplySystemBarStyle(
    window: Window,
    darkMode: Boolean,
) {
    SideEffect {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkMode
            isAppearanceLightNavigationBars = !darkMode
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    ComposeAmethystTheme {
        App()
    }
}
