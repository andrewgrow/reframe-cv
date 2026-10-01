package ankideckbuilder.ui.components.projects

sealed interface ProjectsIntent {
    data object LoadProjects : ProjectsIntent
}
