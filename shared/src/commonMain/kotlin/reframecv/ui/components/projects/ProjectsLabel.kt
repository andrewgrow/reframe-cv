package reframecv.ui.components.projects

import reframecv.domain.models.project.Project

sealed interface ProjectsLabel {
    data object Saving : ProjectsLabel
    data class Created(val project: Project) : ProjectsLabel
    data object Saved : ProjectsLabel
    data object SaveFailed : ProjectsLabel
    data object Deleting : ProjectsLabel
    data object Deleted : ProjectsLabel
    data object DeleteFailed : ProjectsLabel
}
