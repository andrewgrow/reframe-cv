package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import reframecv.domain.models.resume.content.sampleResumeContent
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeImportState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ResumeImportScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun importingMatchesReference() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent(ResumeDraft("Mobile"))
        component.importComponent.state.value = ResumeImportState.Importing
        setGoldenContent(this) { ResumeEditorContent(component) }
        captureGolden(this)
    }

    @Test
    fun failedImportInLightThemeMatchesReference() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent(ResumeDraft("Preserved draft"))
        component.importComponent.state.value = ResumeImportState.Failed
        setGoldenContent(this, themeMode = ThemeMode.Light) { ResumeEditorContent(component) }
        captureGolden(this)
    }

    @Test
    fun importedFieldsMatchReference() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent(
            ResumeDraft("Imported resume").withImportedContent(sampleResumeContent()),
        )
        component.importComponent.state.value = ResumeImportState.Imported
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithText("Cancel").performClick()
        captureGolden(this)
    }
}
