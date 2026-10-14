package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ResumeSectionRemoveConfirmationScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun simpleRemovalMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this) { ResumeSectionRemoveConfirmation("Skills", true, {}, {}) }
        captureGolden(this, "resume.section.remove.dialog")
    }

    @Test
    fun simpleRemovalInLightThemeMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this, themeMode = ThemeMode.Light) {
            ResumeSectionRemoveConfirmation("Summary", true, {}, {})
        }
        captureGolden(this, "resume.section.remove.dialog")
    }
}
