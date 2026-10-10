package reframecv.ui.compose.projects.vacancies

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.domain.models.project.Project
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.application.navigation.ProjectTreeState
import reframecv.ui.components.projects.vacancies.TestVacanciesComponent
import reframecv.ui.components.projects.vacancies.VacanciesState
import reframecv.ui.compose.application.RootContent
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.theme.ReframeTheme.ThemeMode
import reframecv.ui.theme.UiScale

@OptIn(ExperimentalTestApi::class)
class VacanciesContentScreenshotTest : GoldenScreenshotTest() {
    private fun root(state: VacanciesState = VacanciesState.Ready(vacancySamples())) =
        TestRootComponent(
            vacanciesComponent = TestVacanciesComponent(initialState = state),
            initialTreeState = ProjectTreeState(
                projects = listOf(Project(id = 1, name = "Backend", createdAt = 0, updatedAt = 0)),
                selectedId = 1,
                loading = false,
            ),
        )

    @Test
    fun populatedListInDarkThemeMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this) { RootContent(root()) }
    }

    @Test
    fun populatedListInLightThemeMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this, themeMode = ThemeMode.Light) { RootContent(root()) }
    }

    @Test
    fun emptyListMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this) { RootContent(root(VacanciesState.Ready(emptyList()))) }
    }

    @Test
    fun failedLoadMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this, themeMode = ThemeMode.Light) {
            RootContent(root(VacanciesState.LoadFailed))
        }
    }

    @Test
    fun narrowListMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this, screenSize = ScreenSize(320.dp, 600.dp)) {
            VacanciesContent(
                TestVacanciesComponent(initialState = VacanciesState.Ready(vacancySamples())),
            )
        }
    }

    @Test
    fun increasedScaleMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this) {
            ReframeTheme(themeMode = ThemeMode.Dark, uiScale = UiScale(150)) { RootContent(root()) }
        }
    }
}
