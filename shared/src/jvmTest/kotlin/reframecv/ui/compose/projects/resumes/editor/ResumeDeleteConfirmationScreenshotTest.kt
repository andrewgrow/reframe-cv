package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import reframecv.domain.models.resume.Resume
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeDeleteState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ResumeDeleteConfirmationScreenshotTest : GoldenScreenshotTest() {
    private fun component() = TestResumeEditorComponent(
        initialResume = Resume(
            id = 12,
            projectId = 1,
            name = "Android Developer",
            createdAt = 0,
            updatedAt = 0,
        ),
    ).also { it.onRequestDelete() }

    @Test
    fun emptyConfirmationMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this) { ResumeDeleteConfirmation(component()) }
        captureGolden(this, "resume.delete.dialog")
    }

    @Test
    fun failedDeletionInLightThemeMatchesReference() = runDesktopComposeUiTest {
        val component = component()
        component.deleteState.value = ResumeDeleteState.Failed
        setGoldenContent(this, themeMode = ThemeMode.Light) { ResumeDeleteConfirmation(component) }
        onNodeWithText("Permanently delete").performClick()
        captureGolden(this, "resume.delete.dialog")
    }
}
