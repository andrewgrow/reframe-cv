package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.photos.PhotoCrop
import reframecv.photos.photoBytes
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumePhotoState
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent

@OptIn(ExperimentalTestApi::class)
class ResumePhotoContentTest : GoldenScreenshotTest() {
    @Test
    fun photoVisibilityChangesOnlyPrintingPreferenceAndIsSavedWithDraft() =
        runDesktopComposeUiTest {
            var saved: ResumeDraft? = null
            val component = TestResumeEditorComponent(
                initialDraft = ResumeDraft("Mobile", photoPath = "saved"),
                save = { saved = it },
            )
            setGoldenContent(this) { ResumeEditorContent(component) }
            val toggle = onNodeWithTag("resume.photo.visibility")
            toggle.performScrollTo().assertIsOn().performClick()
            toggle.assertIsOff()
            assertEquals("saved", component.draft.value.photoPath)
            onNodeWithText("Save").performClick()
            assertEquals(false, saved?.photoVisible)
            assertEquals("saved", saved?.toResume(1)?.photoPath)
            runOnIdle { component.saveState.value = ResumeSaveState.Saving }
            toggle.assertIsNotEnabled()
        }

    @Test
    fun cropControlsResetAndLockDuringConfirmation() = runDesktopComposeUiTest {
        val component = reframecv.ui.components.projects.resumes.editor.TestResumePhotoComponent()
        component.state.value = ResumePhotoState("source", photoBytes())
        setGoldenContent(this) { ResumePhotoDialog(component) }
        onNodeWithContentDescription("Zoom out").assertIsNotEnabled()
        onNodeWithContentDescription("Zoom in").performClick()
        onNodeWithContentDescription("Move photo right").performClick()
        onNodeWithContentDescription("Move photo down").performClick()
        assertEquals(PhotoCrop(1.1f, 0.1f, 0.1f), component.state.value.crop)
        onNodeWithText("Reset").performClick()
        assertEquals(PhotoCrop(), component.state.value.crop)
        onNodeWithText("Delete").performClick()
        onNodeWithContentDescription("Zoom in").assertIsNotEnabled()
        onNodeWithContentDescription("Move photo right").assertIsNotEnabled()
    }

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
