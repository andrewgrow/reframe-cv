package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.resume.Resume
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeExportState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.TestResumeExportComponent

@OptIn(ExperimentalTestApi::class)
class ResumeExportDialogTest : GoldenScreenshotTest() {
    @Test
    fun exportSavesThenClosesWithoutLeavingResume() = runDesktopComposeUiTest {
        var saves = 0
        var closes = 0
        val component = TestResumeEditorComponent(
            initialResume = Resume(
                id = 1,
                projectId = 1,
                name = "Mobile",
                createdAt = 0,
                updatedAt = 0,
            ),
            save = { saves++ },
            close = { closes++ },
        )
        setGoldenContent(this) { ResumeEditorContent(component) }
        listOf("Cancel", "Download").forEach { action ->
            onNodeWithText("Export").performClick()
            onNodeWithTag("resume.export.format").assertTextContains("as JSON").performClick()
            onNodeWithTag("resume.export.json").performClick()
            onNodeWithTag("resume.export.format").assertTextContains("as JSON")
            // The page also has a Cancel button, so target the dialog's action.
            onNodeWithTag("resume.export.dialog").assertExists()
            onNode(
                hasText(action) and hasAnyAncestor(hasTestTag("resume.export.dialog")),
            ).performClick()
            onNodeWithTag("resume.export.dialog").assertDoesNotExist()
            onNodeWithTag("resumes.editor").assertExists()
        }
        onNodeWithText("Export").performClick()
        onNodeWithTag("resume.export.dialog").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("resume.export.dialog").assertDoesNotExist()
        assertEquals(3, saves)
        assertEquals(0, closes)
    }

    @Test
    fun spinnerDisablesActionsAndEscapeWhileExportingAndFailureAllowsRetry() =
        runDesktopComposeUiTest {
            var downloads = 0
            var closes = 0
            val component = TestResumeExportComponent { downloads++ }
            component.state.value = ResumeExportState.Exporting
            setGoldenContent(this) { ResumeExportDialog(component) { closes++ } }
            onNodeWithTag("resume.export.progress").assertExists()
            onNodeWithTag("resume.export.format").assertIsNotEnabled()
            onNodeWithText("Download").assertIsNotEnabled()
            onNodeWithText("Cancel").assertIsNotEnabled()
            onNodeWithTag("resume.export.dialog").performKeyInput { pressKey(Key.Escape) }
            assertEquals(0, closes)
            runOnIdle { component.state.value = ResumeExportState.Failed }
            onNodeWithTag("resume.export.progress").assertDoesNotExist()
            onNodeWithText("Could not export the resume. Please try again.").assertExists()
            onNodeWithText("Download").performClick()
            assertEquals(1, downloads)
        }
}
