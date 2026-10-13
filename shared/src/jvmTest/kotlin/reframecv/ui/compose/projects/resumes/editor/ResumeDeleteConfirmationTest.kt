package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.resume.Resume
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeDeleteState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent

@OptIn(ExperimentalTestApi::class)
class ResumeDeleteConfirmationTest : GoldenScreenshotTest() {
    @Test
    fun dialogRequiresDeleteAndEnterConfirmsWithoutClosingManagementPageOnEscape() =
        runDesktopComposeUiTest {
            var closes = 0
            var deletes = 0
            val component = TestResumeEditorComponent(
                initialResume = Resume(
                    id = 12,
                    projectId = 1,
                    name = "Mobile",
                    createdAt = 0,
                    updatedAt = 0,
                ),
                close = { closes++ },
                delete = { deletes++ },
            )
            setGoldenContent(this) { ResumeEditorContent(component) }
            onNodeWithText("Delete").performClick()
            onNodeWithText("Permanently delete").assertIsNotEnabled()
            onNodeWithTag("resume.delete.confirmation").performTextReplacement("DEL")
            onNodeWithText("Permanently delete").assertIsNotEnabled()
            onNodeWithTag("resume.delete.confirmation").performTextReplacement("delete")
            onNodeWithText("Permanently delete").assertIsEnabled()
            onNodeWithTag("resume.delete.confirmation").performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("resume.delete.dialog").assertDoesNotExist()
            assertEquals(0, closes)
            onNodeWithText("Delete").performClick()
            onNodeWithTag("resume.delete.confirmation").performTextReplacement("DeLeTe")
            onNodeWithTag("resume.delete.confirmation").performKeyInput { pressKey(Key.Enter) }
            assertEquals(1, deletes)
        }

    @Test
    fun deletingDisablesConfirmationCancelAndDismissal() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent(
            initialResume = Resume(
                id = 12,
                projectId = 1,
                name = "Mobile",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithText("Delete").performClick()
        runOnIdle { component.deleteState.value = ResumeDeleteState.Deleting }
        onNodeWithText("Permanently delete").assertIsNotEnabled()
        onNodeWithTag("resume.delete.confirmation").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("resume.delete.dialog").assertExists()
        assertEquals(ResumeDeleteState.Deleting, component.deleteState.value)
    }
}
