package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeExportState
import reframecv.ui.components.projects.resumes.editor.TestResumeExportComponent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ResumeExportDialogScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun jsonExportMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this) { ResumeExportDialog(TestResumeExportComponent()) {} }
        captureGolden(this, "resume.export.dialog")
    }

    @Test
    fun jsonExportInLightThemeMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this, themeMode = ThemeMode.Light) {
            ResumeExportDialog(TestResumeExportComponent()) {}
        }
        captureGolden(this, "resume.export.dialog")
    }

    @Test
    fun exportingMatchesReference() = runDesktopComposeUiTest {
        val component = TestResumeExportComponent()
        component.state.value = ResumeExportState.Exporting
        mainClock.autoAdvance = false
        setGoldenContent(this) { ResumeExportDialog(component) {} }
        mainClock.advanceTimeBy(300)
        captureGolden(this, "resume.export.dialog")
    }

    @Test
    fun failedExportInLightThemeMatchesReference() = runDesktopComposeUiTest {
        val component = TestResumeExportComponent()
        component.state.value = ResumeExportState.Failed
        setGoldenContent(this, themeMode = ThemeMode.Light) { ResumeExportDialog(component) {} }
        captureGolden(this, "resume.export.dialog")
    }
}
