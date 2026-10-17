package reframecv.usecase

import kotlinx.coroutines.flow.combine
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.repository.CoverLettersRepository
import reframecv.repository.ResumesRepository
import reframecv.repository.VacanciesRepository

/** Supplies the current workspace collections without presentation state. */
data class WorkspaceOverview(
    val resumes: List<Resume>,
    val vacancies: List<Vacancy>,
    val coverLetters: List<CoverLetter>,
)

/** Combines the three repository streams needed by the project dashboard. */
class ObserveDashboardUseCase(
    private val resumes: ResumesRepository,
    private val vacancies: VacanciesRepository,
    private val letters: CoverLettersRepository,
) {
    operator fun invoke(projectId: Long) = combine(
        resumes.observeResumes(projectId),
        vacancies.observeVacancies(projectId),
        letters.observeCoverLetters(projectId),
        ::WorkspaceOverview,
    )
}
