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

/** Lets the vacancy editor show progress, report failures, and close after success. */
internal enum class VacanciesLabel { Saving, Saved, SaveFailed, Deleting, Deleted, DeleteFailed }
