package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import kotlin.test.Test
import reframecv.domain.models.vacancy.Vacancy
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class VacancyEditScreenshotTest : GoldenScreenshotTest() {
    private fun component() = TestVacancyEditorComponent(
        initialVacancy = Vacancy(
            id = 1, projectId = 7, name = "Senior Android Developer", company = "Example",
            description = "Build a Kotlin application.", url = "https://example.com/job",
            keywords = listOf("Kotlin", "Compose"), createdAt = 100, updatedAt = 200,
        ),
    )

    @Test
    fun editDialogMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this) { VacancyEditorContent(component()) }
        onNodeWithText("Cancel").performClick()
        captureGolden(this, VACANCY_EDITOR_TAG)
    }

    @Test
    fun failedUpdateInLightThemeMatchesReference() = runDesktopComposeUiTest {
        val component = component()
        setGoldenContent(this, themeMode = ThemeMode.Light) { VacancyEditorContent(component) }
        onNodeWithTag("vacancy.title").performTextReplacement("Lead Android Developer")
        runOnIdle { component.saveState.value = VacancySaveState.Failed }
        onNodeWithText("Could not save the vacancy. Please try again.").performScrollTo()
        onNodeWithText("Cancel").performClick()
        captureGolden(this, VACANCY_EDITOR_TAG)
    }
}
