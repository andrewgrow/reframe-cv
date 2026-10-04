package reframecv.ui.components.projects

import reframecv.domain.models.project.Project

sealed interface UiState {
    data object Loading : UiState
    data object NoProjects : UiState
    data class Projects(val projects: List<Project>) : UiState
    data object LoadFailed : UiState
}
