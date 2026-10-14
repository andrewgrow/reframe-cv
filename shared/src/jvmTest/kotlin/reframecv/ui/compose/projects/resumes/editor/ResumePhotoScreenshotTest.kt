package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.photos.PhotoCrop
import reframecv.photos.framingPhotoBytes
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumePhotoState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.TestResumePhotoComponent
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ResumePhotoScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun photoStatesMatchReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this) {
            Row(Modifier.padding(ReframeTheme.tokens.spacing.medium)) {
                listOf(ResumePhotoState(), ResumePhotoState("missing"), photo()).forEach { state ->
                    val component = TestResumePhotoComponent()
                    component.state.value = state
                    ResumePhotoBlock(component, enabled = true)
                }
            }
        }
    }

    @Test
    fun selectedPhotoDialogMatchesReference() = dialog(photo())

    @Test
    fun inlineRemovalInLightThemeMatchesReference() =
        dialog(photo(), ThemeMode.Light, confirming = true)

    @Test
    fun missingPhotoMatchesReference() = dialog(ResumePhotoState("missing"), ThemeMode.Light)

    @Test
    fun preparingPhotoMatchesReference() = dialog(ResumePhotoState(loading = true))

    @Test
    fun editorWithSelectedPhotoMatchesReference() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent()
        component.photoComponent.state.value = photo()
        setAndCaptureGolden(this) { ResumeEditorContent(component) }
    }

    @Test
    fun adjustedPhotoMatchesReference() = dialog(photo().copy(crop = PhotoCrop(2f, -0.5f, 0.5f)))

    @Test
    fun narrowPhotoDialogMatchesReference() = runDesktopComposeUiTest(ScreenSize(320.dp, 600.dp)) {
        val component = TestResumePhotoComponent()
        component.state.value = photo()
        setAndCaptureGolden(this, ScreenSize(320.dp, 600.dp), "resume.photo.dialog") {
            ResumePhotoDialog(component)
        }
    }

    private fun photo() = ResumePhotoState("saved", framingPhotoBytes())

    private fun dialog(
        state: ResumePhotoState,
        theme: ThemeMode = ThemeMode.Dark,
        confirming: Boolean = false,
    ) = runDesktopComposeUiTest {
        val component = TestResumePhotoComponent()
        component.state.value = state
        component.confirmingDelete.value = confirming
        setAndCaptureGolden(this, captureTag = "resume.photo.dialog", themeMode = theme) {
            ResumePhotoDialog(component)
        }
    }
}
