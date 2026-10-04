package reframecv.ui.compose.projects.editor

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.editor.EditorSaveState
import reframecv.ui.components.projects.editor.TestEditorComponent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class EditorUpdateScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun failedProjectUpdateInLightThemeMatchesReference() = runDesktopComposeUiTest {
        val component = TestEditorComponent(initialName = "Android Developer")
        component.saveState.value = EditorSaveState.Failed
        setAndCaptureGolden(this, captureTag = PROJECT_EDITOR_TAG, themeMode = ThemeMode.Light) {
            EditorContent(component)
        }
    }

    @Test
    fun updateProjectMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, captureTag = PROJECT_EDITOR_TAG) {
            EditorContent(TestEditorComponent(initialName = "Android Developer"))
        }
    }
}
