package reframecv.ui.components.projects

sealed interface ProjectsIntent {
    data object LoadProjects : ProjectsIntent
}
