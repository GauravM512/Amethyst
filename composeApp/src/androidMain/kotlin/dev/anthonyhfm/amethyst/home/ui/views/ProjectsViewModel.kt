package dev.anthonyhfm.amethyst.home.ui.views

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dev.anthonyhfm.amethyst.core.util.BaseViewModel
import dev.anthonyhfm.amethyst.core.util.Zip
import dev.anthonyhfm.amethyst.core.util.ZippedProjectFormat
import dev.anthonyhfm.amethyst.core.util.determineFormat
import dev.anthonyhfm.amethyst.home.data.HomeRepository
import dev.anthonyhfm.amethyst.home.nav.HomeNavRoute
import dev.anthonyhfm.amethyst.workspace.data.RecentWorkspace
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch

class ProjectsViewModel(
    private val navigator: NavHostController,
    private val snackbarHostState: SnackbarHostState,
) : BaseViewModel<Nothing?, ProjectsViewContract.Event, ProjectsViewContract.Effect>(null) {

    override fun onEvent(event: ProjectsViewContract.Event) {
        when (event) {
            is ProjectsViewContract.Event.OnClickOpenProject -> {
                viewModelScope.launch {
                    val file = FileKit.openFilePicker(
                        type = FileKitType.File(
                            extensions = listOf("ame", "als", "zip", "approj")
                        ),
                        title = "Open Project File",
                    )

                    if (file == null) return@launch

                    when (file.extension.lowercase()) {
                        "ame" -> {
                            runWorkspaceLoad(
                                loadingText = "Loading Project",
                                errorMessage = "Invalid Amethyst Project File",
                            ) {
                                val workspace = HomeRepository.loadWorkspaceData(file)
                                HomeRepository.openWorkspace(workspace)
                            }
                        }

                        "als" -> {
                            navigator.navigate(HomeNavRoute.AbletonImportWizard(file.absolutePath()))
                        }

                        "approj" -> {
                            runWorkspaceLoad(
                                loadingText = "Translating your Apollo Project",
                                errorMessage = "Failed to convert Apollo Project",
                                printStackTrace = true,
                            ) {
                                val workspace = HomeRepository.loadWorkspaceData(file)
                                HomeRepository.openWorkspace(workspace)
                            }
                        }

                        "zip" -> {
                            val format = Zip.determineFormat(file)

                            when (format) {
                                ZippedProjectFormat.ABLETON -> {
                                    navigator.navigate(HomeNavRoute.AbletonImportWizard(file.path))
                                }

                                ZippedProjectFormat.ABLETON_APOLLO -> {
                                    runWorkspaceLoad(
                                        loadingText = "Translating your Ableton + Apollo Project",
                                        errorMessage = "Failed to convert Ableton + Apollo Project",
                                        printStackTrace = true,
                                    ) {
                                        val workspace = HomeRepository.loadWorkspaceData(file)
                                        HomeRepository.openWorkspace(workspace)
                                    }
                                }

                                ZippedProjectFormat.UNIPAD -> {
                                    runWorkspaceLoad(
                                        loadingText = "Translating your UniPad Project",
                                        errorMessage = "Failed to convert UniPad Project",
                                    ) {
                                        val workspace = HomeRepository.loadWorkspaceData(file)
                                        HomeRepository.openWorkspace(workspace)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is ProjectsViewContract.Event.OnClickNewProject -> {
                triggerEffect(ProjectsViewContract.Effect.ShowCreateSheet)
            }

            is ProjectsViewContract.Event.OpenProjectFromHistory -> {
                viewModelScope.launch {
                    runWorkspaceLoad(
                        loadingText = "Opening Project",
                        errorMessage = "Failed to open recent project",
                        printStackTrace = true,
                    ) {
                        HomeRepository.openRecentWorkspace(event.project)
                    }
                }
            }

            is ProjectsViewContract.Event.OnClickEditProject -> {
                triggerEffect(ProjectsViewContract.Effect.ShowEditSheet(event.project.path))
            }

            is ProjectsViewContract.Event.OnClickDeleteProject -> {
                HomeRepository.removeRecentWorkspace(event.path)
            }
        }
    }

    private suspend fun runWorkspaceLoad(
        loadingText: String? = null,
        errorMessage: String,
        printStackTrace: Boolean = false,
        block: suspend () -> Unit,
    ) {
        if (loadingText != null) {
            navigator.navigate(HomeNavRoute.LoadingScreen(loadingText))
        }

        try {
            block()
            triggerEffect(ProjectsViewContract.Effect.OpenWorkspace)
        } catch (exception: Exception) {
            if (loadingText != null) {
                navigator.popBackStack()
            }

            if (printStackTrace) {
                exception.printStackTrace()
            }

            snackbarHostState.showSnackbar(
                message = errorMessage,
                withDismissAction = true,
            )
        }
    }
}

sealed interface ProjectsViewContract {
    sealed interface Event {
        data object OnClickOpenProject : Event
        data object OnClickNewProject : Event

        data class OpenProjectFromHistory(
            val project: RecentWorkspace,
        ) : Event

        data class OnClickEditProject(
            val project: RecentWorkspace,
        ) : Event

        data class OnClickDeleteProject(
            val path: String,
        ) : Event
    }

    sealed interface Effect {
        data object OpenWorkspace : Effect
        data object ShowCreateSheet : Effect
        data class ShowEditSheet(val projectPath: String) : Effect
    }
}
