package ankideckbuilder.ui.components.projects

sealed interface UiState {
    data object NoProjects : UiState
}
