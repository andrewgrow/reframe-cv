package reframecv.ui.components.projects.coverletters

import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.coverletter.CoverLetterVacancyLink
import reframecv.domain.models.vacancy.Vacancy

/** Provides letters and the vacancies to which they are attached. */
sealed interface CoverLettersState {
    data object Loading : CoverLettersState
    data object LoadFailed : CoverLettersState

    /** Retains current vacancies so links follow edits and removals. */
    data class Ready(val letters: List<CoverLetter>, val vacancies: List<Vacancy> = emptyList()) :
        CoverLettersState {
        val linkedVacancies: List<CoverLetterVacancyLink> get() = vacancies.sortedBy { it.id }
            .flatMap { vacancy ->
                letters.filter { it.id in vacancy.coverLetterIds }.map {
                    CoverLetterVacancyLink(it.id, vacancy.id, vacancy.name)
                }
            }
    }
}
