package reframecv.ui.components.projects

sealed interface ProjectsIntent {
    data object LoadProjects : ProjectsIntent
    data class CreateProject(val name: String) : ProjectsIntent
    data class UpdateProject(val id: Long, val name: String) : ProjectsIntent
}
