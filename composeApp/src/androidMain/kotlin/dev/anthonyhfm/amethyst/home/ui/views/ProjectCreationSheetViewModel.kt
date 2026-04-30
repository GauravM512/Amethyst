package dev.anthonyhfm.amethyst.home.ui.views

import androidx.lifecycle.viewModelScope
import dev.anthonyhfm.amethyst.core.util.BaseViewModel
import dev.anthonyhfm.amethyst.home.data.HomeRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ProjectCreationSheetViewModel(
    private val projectPath: String? = null,
) : BaseViewModel<ProjectCreationSheetContract.State, ProjectCreationSheetContract.Event, ProjectCreationSheetContract.Effect>(
    initialState = if (projectPath != null) {
        loadProjectData(projectPath)
    } else {
        ProjectCreationSheetContract.State(
            name = "",
            author = HomeRepository.localAuthor(),
            projectPath = null,
        )
    }
) {
    companion object {
        private fun loadProjectData(path: String): ProjectCreationSheetContract.State {
            val project = runBlocking { HomeRepository.loadProjectDetails(path) }
            return if (project != null) {
                ProjectCreationSheetContract.State(
                    name = project.name,
                    author = project.author,
                    isNameValid = project.name.isNotBlank(),
                    projectPath = project.projectPath,
                )
            } else {
                ProjectCreationSheetContract.State(
                    name = "",
                    author = HomeRepository.localAuthor(),
                    projectPath = null,
                )
            }
        }
    }

    override fun onEvent(event: ProjectCreationSheetContract.Event) {
        when (event) {
            is ProjectCreationSheetContract.Event.OnChangeName -> {
                updateState(
                    state.value.copy(
                        name = event.value,
                        isNameValid = event.value.isNotBlank(),
                    )
                )
            }

            is ProjectCreationSheetContract.Event.OnChangeAuthor -> {
                updateState(state.value.copy(author = event.value))
            }

            is ProjectCreationSheetContract.Event.OnClickSubmit -> {
                if (!state.value.isNameValid) return

                val currentState = state.value
                viewModelScope.launch {
                    if (currentState.projectPath != null) {
                        HomeRepository.updateProject(
                            path = currentState.projectPath,
                            name = currentState.name,
                            author = currentState.author,
                        )
                    } else {
                        HomeRepository.createProject(
                            name = currentState.name,
                            author = currentState.author,
                        )
                    }
                    triggerEffect(ProjectCreationSheetContract.Effect.OpenWorkspace)
                }
            }
        }
    }
}

sealed interface ProjectCreationSheetContract {
    sealed interface Event {
        data class OnChangeName(val value: String) : Event
        data class OnChangeAuthor(val value: String) : Event
        data object OnClickSubmit : Event
    }

    sealed interface Effect {
        data object OpenWorkspace : Effect
    }

    data class State(
        val name: String,
        val author: String,
        val isNameValid: Boolean = false,
        val projectPath: String? = null,
    )
}
