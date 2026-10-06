package reframecv.ui.components.projects.dashboard

import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy

sealed interface DashboardState {
    data object Loading : DashboardState
    data object LoadFailed : DashboardState
    data class Ready(
        val resumes: List<Resume> = emptyList(),
        val vacancies: List<Vacancy> = emptyList(),
        val coverLetters: List<CoverLetter> = emptyList(),
    ) : DashboardState
}
