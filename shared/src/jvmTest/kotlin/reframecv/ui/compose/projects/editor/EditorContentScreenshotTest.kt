package reframecv.ui.compose.projects.editor

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import kotlin.test.Test
import org.jetbrains.compose.resources.getString
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.action_create
import reframecv.shared.generated.resources.project_name
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.editor.TestEditorComponent

private const val PROJECT_NAME = "Test Project"

@OptIn(ExperimentalTestApi::class)
class EditorContentScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun emptyProjectNameWithCancelFocusedMatchesReference() = runDesktopComposeUiTest {
        val projectNameLabel = getString(Res.string.project_name)
        val cancelLabel = getString(Res.string.action_cancel)
        val createLabel = getString(Res.string.action_create)
        setGoldenContent(this) { EditorContent(TestEditorComponent()) }

        onNodeWithText(projectNameLabel).assertIsFocused()
        onNodeWithText(cancelLabel).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText(cancelLabel).assertIsFocused()
        onNodeWithText(projectNameLabel).assertIsNotFocused().assertIsDisplayed()
        onNodeWithText(createLabel).assertIsNotEnabled()
        captureGolden(this, captureTag = PROJECT_EDITOR_TAG)
    }

    @Test
    fun filledProjectNameMatchesReference() = runDesktopComposeUiTest {
        val component = TestEditorComponent()
        val projectNameLabel = getString(Res.string.project_name)
        setGoldenContent(this) { EditorContent(component) }

        onNodeWithText(projectNameLabel).performTextReplacement(PROJECT_NAME)
        captureGolden(this, captureTag = PROJECT_EDITOR_TAG)
    }
}
