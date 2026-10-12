package reframecv.ui.components.projects.resumes

import reframecv.domain.models.resume.Resume

/** Identifies loading, failure, or the project's current resume list. */
sealed interface ResumesState {
    data object Loading : ResumesState
    data object LoadFailed : ResumesState

    /** Supplies the loaded resumes. */
    data class Ready(val resumes: List<Resume>) : ResumesState
}
