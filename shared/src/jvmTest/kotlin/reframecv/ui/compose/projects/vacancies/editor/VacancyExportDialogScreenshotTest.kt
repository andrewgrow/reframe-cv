package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyExportComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyExportFormat
import reframecv.ui.components.projects.vacancies.editor.VacancyExportState
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class VacancyExportDialogScreenshotTest : GoldenScreenshotTest() {
    @Test fun jsonExportMatchesReference() = capture(
        VacancyExportState.Idle,
        format = VacancyExportFormat.Json,
    )

    @Test fun pdfExportMatchesReference() = capture(VacancyExportState.Idle)

    @Test fun exportingMatchesReference() = capture(VacancyExportState.Exporting)

    @Test fun failedExportInLightThemeMatchesReference() =
        capture(VacancyExportState.Failed, ThemeMode.Light)

    @Test
    fun fullSourceZipShowsNamingAndLinkedDocumentChoices() = runDesktopComposeUiTest {
        val component = archiveExportFixture()
        setAndCaptureGolden(this, captureTag = "vacancy.export.dialog") {
            VacancyExportDialog(component) {}
        }
    }

    @Test
    fun fullSourceZipWithoutLinksStillAllowsVacancyOnlyExport() = runDesktopComposeUiTest {
        val component = archiveExportFixture()
        component.archive.value = component.archive.value.copy(
            resumes = emptyList(),
            letters = emptyList(),
            resumeIds = emptySet(),
            letterIds = emptySet(),
        )
        setAndCaptureGolden(
            this,
            captureTag = "vacancy.export.dialog",
            themeMode = ThemeMode.Light,
        ) {
            VacancyExportDialog(component) {}
        }
    }

    private fun capture(
        state: VacancyExportState,
        theme: ThemeMode = ThemeMode.Dark,
        format: VacancyExportFormat = VacancyExportFormat.Pdf,
    ) = runDesktopComposeUiTest {
        val component = TestVacancyExportComponent()
        component.state.value = state
        component.format.value = format
        setAndCaptureGolden(this, captureTag = "vacancy.export.dialog", themeMode = theme) {
            VacancyExportDialog(component) {}
        }
    }
}
