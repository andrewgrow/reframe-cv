package reframecv.ui.components.projects.vacancies

import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft

/** Lets vacancy UI request changes without performing storage work itself. */
internal sealed interface VacanciesIntent {
    data object Load : VacanciesIntent

    /** Submits a new vacancy draft. */
    data class Create(val draft: VacancyDraft) : VacanciesIntent

    /** Requests permanent deletion of a vacancy. */
    data class Delete(val id: Long) : VacanciesIntent

    /** Submits changes to an existing vacancy. */
    data class Update(val vacancy: Vacancy, val draft: VacancyDraft) : VacanciesIntent
}

/** Reports vacancy operations and supplies the saved identity after creation. */
internal sealed interface VacanciesLabel {
    data object Saving : VacanciesLabel
    data class Saved(val createdVacancy: Vacancy? = null) : VacanciesLabel
    data object SaveFailed : VacanciesLabel
    data object Deleting : VacanciesLabel
    data object Deleted : VacanciesLabel
    data object DeleteFailed : VacanciesLabel
}
