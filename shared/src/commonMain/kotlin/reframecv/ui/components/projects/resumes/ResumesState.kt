package reframecv.ui.components.projects.resumes

import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.ResumeVacancyLink

/** Identifies loading, failure, or the project's current resume list. */
sealed interface ResumesState {
    data object Loading : ResumesState
    data object LoadFailed : ResumesState

    /** Supplies the loaded resumes. */
    data class Ready(
        val resumes: List<Resume>,
        val mainResumeId: Long? = null,
        val linkedVacancies: List<ResumeVacancyLink> = emptyList(),
    ) : ResumesState
}
