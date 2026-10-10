package reframecv.ui.components.projects.vacancies

import reframecv.domain.models.vacancy.Vacancy

sealed interface VacanciesState {
    data object Loading : VacanciesState
    data object LoadFailed : VacanciesState
    data class Ready(val vacancies: List<Vacancy>) : VacanciesState
}
