package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import reframecv.domain.models.vacancy.Vacancy
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class VacancyDeleteScreenshotTest : GoldenScreenshotTest() {
    private fun component() = TestVacancyEditorComponent(
        initialVacancy = Vacancy(
            id = 1,
            projectId = 7,
            name = "Backend Developer",
            createdAt = 100,
            updatedAt = 200,
        ),
    )

    @Test
    fun deleteConfirmationMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this) { VacancyEditorContent(component()) }
        onNodeWithText("Delete").performClick()
        captureGolden(this, VACANCY_EDITOR_TAG)
    }

    @Test
    fun failedDeletionInLightThemeMatchesReference() = runDesktopComposeUiTest {
        val component = component()
        setGoldenContent(this, themeMode = ThemeMode.Light) { VacancyEditorContent(component) }
        onNodeWithText("Delete").performClick()
        runOnIdle { component.deleteState.value = VacancyDeleteState.Failed }
        captureGolden(this, VACANCY_EDITOR_TAG)
    }
}
