package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.domain.models.vacancy.Vacancy
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.projects.vacancies.TestVacanciesComponent
import reframecv.ui.compose.application.RootContent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class VacancyPageScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun creationWithProjectTree() = runDesktopComposeUiTest {
        val vacancies = TestVacanciesComponent()
        vacancies.onAdd()
        setAndCaptureGolden(this) {
            RootContent(TestRootComponent(vacanciesComponent = vacancies))
        }
    }

    @Test
    fun editingWithProjectTreeInLightTheme() = runDesktopComposeUiTest {
        val vacancies = TestVacanciesComponent()
        vacancies.onEdit(vacancy())
        setAndCaptureGolden(this, themeMode = ThemeMode.Light) {
            RootContent(TestRootComponent(vacanciesComponent = vacancies))
        }
    }

    @Test
    fun narrowEditorWithProjectTree() {
        val size = ScreenSize(600.dp, 600.dp)
        runDesktopComposeUiTest(size) {
            val vacancies = TestVacanciesComponent()
            vacancies.onEdit(vacancy())
            setAndCaptureGolden(this, size) {
                RootContent(TestRootComponent(vacanciesComponent = vacancies))
            }
        }
    }

    private fun vacancy() = Vacancy(
        id = 1, projectId = 1, name = "Senior Android Developer",
        company = "Example", description = "Build a Kotlin application.",
        url = "https://example.com/job", keywords = listOf("Kotlin", "Compose"),
        createdAt = 100, updatedAt = 200,
    )
}
