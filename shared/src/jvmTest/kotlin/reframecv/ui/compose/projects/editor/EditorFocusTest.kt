package reframecv.ui.compose.projects.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.action_update
import reframecv.shared.generated.resources.project_name
import reframecv.testing.getTestString
import reframecv.ui.components.projects.editor.TestEditorComponent

private const val PROJECT_NAME = "Test Project"

@OptIn(ExperimentalTestApi::class)
class EditorFocusTest {
    private val projectNameLabel = getTestString(Res.string.project_name)
    private val saveLabel = getTestString(Res.string.action_update)

    @Test
    fun tabsThroughUpdateCancelDeleteAndReturnsCursorToEnd() = runComposeUiTest {
        setContent { EditorContent(TestEditorComponent(initialName = PROJECT_NAME)) }
        val field = onNodeWithText(projectNameLabel)
        field.assertIsFocused()
        assertEquals(
            TextRange(PROJECT_NAME.length),
            field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange],
        )
        field.performTextInputSelection(TextRange(0))
        field.performKeyInput { pressKey(Key.Tab) }
        onNodeWithText(saveLabel).assertIsFocused().performKeyInput { pressKey(Key.Tab) }
        onNodeWithText(getTestString(Res.string.action_cancel)).assertIsFocused().performKeyInput {
            pressKey(Key.Tab)
        }
        onNodeWithText(getTestString(Res.string.action_delete)).assertIsFocused().performKeyInput {
            pressKey(Key.Tab)
        }
        field.assertIsFocused()
        assertEquals(
            TextRange(PROJECT_NAME.length),
            field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange],
        )
    }

    @Test
    fun tabsPastDisabledUpdateForBlankName() = runComposeUiTest {
        setContent { EditorContent(TestEditorComponent(initialName = PROJECT_NAME)) }
        onNodeWithText(projectNameLabel).performTextReplacement("")
        onNodeWithText(projectNameLabel).performKeyInput {
            pressKey(Key.Tab)
        }
        onNodeWithText(getTestString(Res.string.action_cancel)).assertIsFocused().performKeyInput {
            pressKey(Key.Tab)
        }
        onNodeWithText(getTestString(Res.string.action_delete)).assertIsFocused().performKeyInput {
            pressKey(Key.Tab)
        }
        onNodeWithText(projectNameLabel).assertIsFocused()
    }
}
