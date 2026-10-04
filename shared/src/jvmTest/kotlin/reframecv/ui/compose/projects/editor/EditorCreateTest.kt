package reframecv.ui.compose.projects.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_create
import reframecv.shared.generated.resources.project_name
import reframecv.testing.getTestString
import reframecv.ui.components.projects.editor.TestEditorComponent

private const val PROJECT_NAME = "Test Project"

@OptIn(ExperimentalTestApi::class)
class EditorCreateTest {
    private val projectNameLabel = getTestString(Res.string.project_name)
    private val createLabel = getTestString(Res.string.action_create)

    @Test
    fun validatesAndTrimsProjectName() = runComposeUiTest {
        var savedName: String? = null
        setContent {
            EditorContent(TestEditorComponent(), onSave = { savedName = it })
        }

        onNodeWithText(projectNameLabel).assertIsFocused()
        onNodeWithText(createLabel).assertIsNotEnabled()
        onNodeWithText(projectNameLabel).performTextReplacement("   ")
        onNodeWithText(createLabel).assertIsNotEnabled()
        onNodeWithText(projectNameLabel).performTextReplacement("  $PROJECT_NAME  ")
        onNodeWithText(projectNameLabel).performKeyInput { pressKey(Key.Enter) }
        assertEquals(PROJECT_NAME, savedName)
    }

    @Test
    fun closesWithSingleNonWhitespaceCharacter() = runComposeUiTest {
        var closed = false
        setContent { EditorContent(TestEditorComponent { closed = true }) }

        onNodeWithText(createLabel).assertIsNotEnabled()
        onNodeWithText(projectNameLabel).performTextReplacement("   ")
        onNodeWithText(createLabel).assertIsNotEnabled()
        onNodeWithText(projectNameLabel).performTextReplacement(" ! ")
        onNodeWithText(createLabel).assertIsEnabled()
        onNodeWithText(projectNameLabel).performTextReplacement("")
        onNodeWithText(createLabel).assertIsNotEnabled()
        onNodeWithText(projectNameLabel).performTextReplacement("!")
        onNodeWithText(createLabel).performClick()
        assertTrue(closed)
    }
}
