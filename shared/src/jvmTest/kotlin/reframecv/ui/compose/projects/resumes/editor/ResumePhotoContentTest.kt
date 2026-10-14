package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
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
import reframecv.photos.photoBytes
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumePhotoState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent

@OptIn(ExperimentalTestApi::class)
class ResumePhotoContentTest : GoldenScreenshotTest() {
    @Test
    fun photoOpensOneDialogAndRemovalRequiresInlineConfirmation() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent()
        component.photoComponent.state.value = ResumePhotoState("saved", photoBytes())
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithTag("resume.photo").performClick()
        onNodeWithTag("resume.photo.dialog").assertExists()
        onNodeWithText("Delete").performClick()
        onNodeWithText("Yes, remove photo").assertExists()
        onNodeWithTag("confirmation.input").assertDoesNotExist()
        onNodeWithTag("resume.photo.dialog").performKeyInput { pressKey(Key.Escape) }
        onNodeWithText("Yes, remove photo").assertDoesNotExist()
        onNodeWithTag("resume.photo.dialog").assertExists()
        onNodeWithText("Delete").performClick()
        onNode(
            hasText("Cancel") and hasAnyAncestor(hasTestTag("resume.photo.dialog")),
        ).performClick()
        onNodeWithText("Delete").performClick()
        onNodeWithText("Yes, remove photo").performClick()
        assertEquals(null, component.photoComponent.state.value.path)
        onNodeWithText("Delete").assertDoesNotExist()
        onNodeWithTag("resume.photo.dialog").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("resume.photo.dialog").assertDoesNotExist()
        onNodeWithTag("resumes.editor").assertExists()
    }
}
