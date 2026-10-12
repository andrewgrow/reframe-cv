package reframecv.ui.compose.projects.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.editor.TestEditorComponent

@OptIn(ExperimentalTestApi::class)
class EditorDialogResizeScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun expandedDialogMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this) { EditorContent(TestEditorComponent(initialName = "Backend")) }
        onNodeWithTag("dialog.resize").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(60f, 100f))
            release()
        }
        captureGolden(this, captureTag = PROJECT_EDITOR_TAG)
    }
}
