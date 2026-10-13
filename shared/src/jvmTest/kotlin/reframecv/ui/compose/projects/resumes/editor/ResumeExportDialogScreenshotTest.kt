package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ResumeExportDialogScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun jsonExportMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this) { ResumeExportDialog {} }
        captureGolden(this, "resume.export.dialog")
    }

    @Test
    fun jsonExportInLightThemeMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this, themeMode = ThemeMode.Light) { ResumeExportDialog {} }
        captureGolden(this, "resume.export.dialog")
    }
}
