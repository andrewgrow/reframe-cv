package reframecv.ui.components.projects.vacancies

import reframecv.domain.models.vacancy.Vacancy

/** Lets the vacancy screen choose between loading, its list, and error feedback. */
sealed interface VacanciesState {
    data object Loading : VacanciesState
    data object LoadFailed : VacanciesState

    /** Supplies the loaded vacancy list. */
    data class Ready(val vacancies: List<Vacancy>) : VacanciesState
}
