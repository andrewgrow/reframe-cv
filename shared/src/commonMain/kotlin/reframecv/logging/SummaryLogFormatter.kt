package reframecv.logging

import com.arkivanov.mvikotlin.core.store.StoreEventType
import com.arkivanov.mvikotlin.logging.logger.LogFormatter
import reframecv.ui.components.projects.ProjectsIntent
import reframecv.ui.components.projects.ProjectsLabel
import reframecv.ui.components.projects.UiState
import reframecv.ui.components.projects.dashboard.DashboardState
import reframecv.ui.components.projects.vacancies.VacanciesIntent
import reframecv.ui.components.projects.vacancies.VacanciesState

/** Describes Store events without including document text or project names. */
internal class SummaryLogFormatter : LogFormatter {
    override fun format(storeName: String, eventType: StoreEventType, value: Any?): String =
        "$storeName $eventType ${summarize(value)}"

    private fun summarize(value: Any?): String = when (value) {
        is ProjectsIntent.UpdateProject -> "UpdateProject(projectId=${value.id})"

        is ProjectsIntent.DeleteProject -> "DeleteProject(projectId=${value.id})"

        is ProjectsLabel.Created -> "Created(projectId=${value.project.id})"

        is UiState.Projects -> "Projects(count=${value.projects.size})"

        is VacanciesIntent.Update -> "Update(vacancyId=${value.vacancy.id})"

        is VacanciesIntent.Delete -> "Delete(vacancyId=${value.id})"

        is VacanciesState.Ready -> "Ready(count=${value.vacancies.size})"

        is DashboardState.Ready ->
            "Ready(resumes=${value.resumes.size}, " +
                "vacancies=${value.vacancies.size}, coverLetters=${value.coverLetters.size})"

        is Enum<*> -> value.name

        else -> value?.let { it::class.simpleName } ?: "None"
    }
}
