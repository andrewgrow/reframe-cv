package reframecv.ui.components.projects

/** Lets project UI request changes without performing storage work itself. */
sealed interface ProjectsIntent {
    data object LoadProjects : ProjectsIntent

    /** Requests creation in the current project list. */
    data class CreateProject(val name: String) : ProjectsIntent

    /** Requests a project name change. */
    data class UpdateProject(val id: Long, val name: String) : ProjectsIntent

    /** Requests soft deletion of a project subtree. */
    data class DeleteProject(val id: Long) : ProjectsIntent
}
