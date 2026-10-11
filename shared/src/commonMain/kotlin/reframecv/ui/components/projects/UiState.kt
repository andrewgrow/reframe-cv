package reframecv.ui.components.projects

import reframecv.domain.models.project.Project

/** Lets the project screen choose between loading, an empty prompt, a list, and an error. */
sealed interface UiState {
    data object Loading : UiState
    data object NoProjects : UiState

    /** Carries the loaded projects for the current list. */
    data class Projects(val projects: List<Project>) : UiState
    data object LoadFailed : UiState
}
