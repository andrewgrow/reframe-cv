package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyExportComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyExportState
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class VacancyExportDialogScreenshotTest : GoldenScreenshotTest() {
    @Test fun jsonExportMatchesReference() = capture(VacancyExportState.Idle)

    @Test fun exportingMatchesReference() = capture(VacancyExportState.Exporting)

    @Test fun failedExportInLightThemeMatchesReference() =
        capture(VacancyExportState.Failed, ThemeMode.Light)

    private fun capture(state: VacancyExportState, theme: ThemeMode = ThemeMode.Dark) =
        runDesktopComposeUiTest {
            val component = TestVacancyExportComponent()
            component.state.value = state
            setAndCaptureGolden(this, captureTag = "vacancy.export.dialog", themeMode = theme) {
                VacancyExportDialog(component) {}
            }
        }
}
