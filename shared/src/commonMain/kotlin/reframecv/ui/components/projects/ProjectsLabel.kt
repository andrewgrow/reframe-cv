package reframecv.ui.components.projects

sealed interface ProjectsLabel {
    data object Saving : ProjectsLabel
    data object Saved : ProjectsLabel
    data object SaveFailed : ProjectsLabel
    data object Deleting : ProjectsLabel
    data object Deleted : ProjectsLabel
    data object DeleteFailed : ProjectsLabel
}
