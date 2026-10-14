package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_remove_skills_section
import reframecv.shared.generated.resources.resume_remove_summary_section
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ResumeSectionRemoveConfirmationScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun simpleRemovalMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this) {
            ResumeSectionRemoveConfirmation(
                "Skills",
                Res.string.resume_remove_skills_section,
                true,
                {},
                {},
            )
        }
        captureGolden(this, "resume.section.remove.dialog")
    }

    @Test
    fun simpleRemovalInLightThemeMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this, themeMode = ThemeMode.Light) {
            ResumeSectionRemoveConfirmation(
                "Summary",
                Res.string.resume_remove_summary_section,
                true,
                {},
                {},
            )
        }
        captureGolden(this, "resume.section.remove.dialog")
    }
}
