package reframecv.ui.compose.projects.vacancies

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.TestVacanciesComponent
import reframecv.ui.components.projects.vacancies.VacanciesState
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class VacancySelectionScreenshotTest : GoldenScreenshotTest() {
    private fun fixture() =
        TestVacanciesComponent(initialState = VacanciesState.Ready(vacancySamples()))

    @Test
    fun appliedColumnAppearsBeforeAddedInTheFullWidthTable() = runComposeUiTest {
        setAndCaptureGolden(this) { VacanciesContent(fixture()) }
    }

    @Test
    fun selectedVacanciesInLightTheme() = runComposeUiTest {
        val component = fixture().also { it.selection.onToggle(1) }
        setAndCaptureGolden(this, themeMode = ThemeMode.Light) { VacanciesContent(component) }
    }

    @Test
    fun bulkDeleteRequiresConfirmationWithoutTypingDelete() = runComposeUiTest {
        val component = fixture().also {
            it.selection.onToggleAll()
            it.selection.onRequestDelete()
        }
        setAndCaptureGolden(this, captureTag = "vacancies.delete.dialog") {
            VacanciesContent(component)
        }
    }

    @Test
    fun appliedFilterShowsOnlyUnappliedVacancies() = runComposeUiTest {
        val component = fixture().also { it.selection.onToggleAppliedFilter() }
        setAndCaptureGolden(this) { VacanciesContent(component) }
    }

    @Test
    fun appliedFilterKeepsRestoreControlAvailableWhenEverythingIsHidden() = runComposeUiTest {
        val component = TestVacanciesComponent(
            initialState = VacanciesState.Ready(listOf(vacancySamples().first())),
        ).also { it.selection.onToggleAppliedFilter() }
        setAndCaptureGolden(this, themeMode = ThemeMode.Light) { VacanciesContent(component) }
    }
}
