package reframecv.ui.components.projects.vacancies

import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft

internal sealed interface VacanciesIntent {
    data object Load : VacanciesIntent
    data class Create(val draft: VacancyDraft) : VacanciesIntent
    data class Delete(val id: Long) : VacanciesIntent
    data class Update(val vacancy: Vacancy, val draft: VacancyDraft) : VacanciesIntent
}

internal enum class VacanciesLabel { Saving, Saved, SaveFailed, Deleting, Deleted, DeleteFailed }
