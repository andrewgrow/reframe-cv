package ankideckbuilder.ui.compose.projects.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import ankideckbuilder.shared.generated.resources.Res
import ankideckbuilder.shared.generated.resources.project_name
import ankideckbuilder.testing.GoldenScreenshotTest
import ankideckbuilder.ui.components.projects.editor.TestEditorComponent
import kotlin.test.Test
import org.jetbrains.compose.resources.getString

private const val PROJECT_NAME = "Test Project"

@OptIn(ExperimentalTestApi::class)
class EditorContentScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun filledProjectNameMatchesReference() = runDesktopComposeUiTest {
        val component = TestEditorComponent()
        val projectNameLabel = getString(Res.string.project_name)
        setGoldenContent(this) { EditorContent(component) }

        onNodeWithText(projectNameLabel).performTextReplacement(PROJECT_NAME)
        captureGolden(this, captureTag = PROJECT_EDITOR_TAG)
    }
}
