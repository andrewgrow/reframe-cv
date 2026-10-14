package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.resume.Resume
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeImportState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent

@OptIn(ExperimentalTestApi::class)
class ResumeImportContentTest : GoldenScreenshotTest() {
    @Test
    fun creationOffersImportAndBusyStateBlocksEditingSaveAndBack() = runDesktopComposeUiTest {
        var imports = 0
        var saves = 0
        var closes = 0
        val component = TestResumeEditorComponent(
            ResumeDraft("Mobile"),
            import = { imports++ },
            save = { saves++ },
            close = { closes++ },
        )
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithText("Import").performClick()
        assertEquals(1, imports)
        assertEquals(0, saves)
        runOnIdle { component.importComponent.state.value = ResumeImportState.Importing }
        onNodeWithTag("resume.import.progress").assertExists()
        onNodeWithText("Import").assertIsNotEnabled()
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        onNodeWithTag("resume.name").assertIsNotEnabled()
        onNodeWithTag("resumes.editor").performKeyInput { pressKey(Key.Escape) }
        assertEquals(0, closes)
        runOnIdle { component.importComponent.state.value = ResumeImportState.Failed }
        onNodeWithText(
            "Could not import the resume. Choose a valid Reframe CV JSON file and try again.",
        ).assertExists()
        onNodeWithText("Import").performClick()
        assertEquals(2, imports)
        onNodeWithText("Cancel").performClick()
        assertEquals(1, closes)
        assertEquals(0, saves)
    }

    @Test
    fun managementOffersExportWithoutImport() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent(
            initialResume = Resume(
                id = 1,
                projectId = 1,
                name = "Mobile",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithText("Import").assertDoesNotExist()
        onNodeWithText("Export").assertExists()
    }
}
