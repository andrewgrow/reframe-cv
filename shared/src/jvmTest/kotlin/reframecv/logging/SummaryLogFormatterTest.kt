package reframecv.logging

import com.arkivanov.mvikotlin.core.store.StoreEventType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.dashboard.DashboardState
import reframecv.ui.components.projects.vacancies.VacanciesIntent
import reframecv.ui.components.projects.vacancies.VacanciesState
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft

class SummaryLogFormatterTest {
    private val formatter = SummaryLogFormatter()

    @Test
    fun vacancyAndDashboardSummariesOmitDocumentContents() {
        val vacancy = Vacancy(
            id = 12,
            projectId = 7,
            name = "Private title",
            description = "Private description",
            createdAt = 0,
            updatedAt = 0,
        )
        val values = listOf(
            VacanciesIntent.Create(VacancyDraft("Private draft")) to "Create",
            VacanciesIntent.Update(vacancy, VacancyDraft("Private changes")) to
                "Update(vacancyId=12)",
            VacanciesIntent.Delete(12) to "Delete(vacancyId=12)",
            VacanciesState.Ready(listOf(vacancy)) to "Ready(count=1)",
            DashboardState.Ready(vacancies = listOf(vacancy)) to
                "Ready(resumes=0, vacancies=1, coverLetters=0)",
        )
        values.forEach { (value, expected) ->
            val text = formatter.format("Store(projectId=7)", StoreEventType.STATE, value)
            assertContains(text, expected)
            assertFalse(text.contains("Private"))
        }
    }

    @Test
    fun unknownPayloadsNeverCallToString() {
        val value = object {
            override fun toString(): String = error("Must not format raw payload")
        }
        formatter.format("FutureStore", StoreEventType.INTENT, value)
    }
}
