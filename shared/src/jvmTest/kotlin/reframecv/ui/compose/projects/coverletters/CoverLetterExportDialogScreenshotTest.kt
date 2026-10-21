package reframecv.ui.compose.projects.coverletters

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.coverletters.CoverLetterExportFormat
import reframecv.ui.components.projects.coverletters.CoverLetterExportState
import reframecv.ui.components.projects.coverletters.TestCoverLetterExportComponent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class CoverLetterExportDialogScreenshotTest : GoldenScreenshotTest() {
    @Test fun pdfExportMatchesReference() = capture(CoverLetterExportState.Idle)

    @Test fun jsonExportMatchesReference() = capture(
        CoverLetterExportState.Idle,
        format = CoverLetterExportFormat.Json,
    )

    @Test fun exportingMatchesReference() = capture(CoverLetterExportState.Exporting)

    @Test fun failedExportInLightThemeMatchesReference() =
        capture(CoverLetterExportState.Failed, ThemeMode.Light)

    private fun capture(
        state: CoverLetterExportState,
        theme: ThemeMode = ThemeMode.Dark,
        format: CoverLetterExportFormat = CoverLetterExportFormat.Pdf,
    ) = runDesktopComposeUiTest {
        val component = TestCoverLetterExportComponent()
        component.onFormatChange(format)
        component.state.value = state
        setAndCaptureGolden(
            this,
            captureTag = "cover_letter.export.dialog",
            themeMode = theme,
        ) {
            CoverLetterExportDialog(component) {}
        }
    }
}
