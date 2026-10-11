package reframecv.ui.components.projects

import reframecv.domain.models.project.Project

/** Lets the editor react to progress and failures and navigation open newly created projects. */
sealed interface ProjectsLabel {
    data object Saving : ProjectsLabel

    /** Lets navigation open the project immediately after creation. */
    data class Created(val project: Project) : ProjectsLabel
    data object Saved : ProjectsLabel
    data object SaveFailed : ProjectsLabel
    data object Deleting : ProjectsLabel
    data object Deleted : ProjectsLabel
    data object DeleteFailed : ProjectsLabel
}
