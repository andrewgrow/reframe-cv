package reframecv.ui.compose.projects.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_update
import reframecv.shared.generated.resources.project_editor_edit_title
import reframecv.shared.generated.resources.project_name
import reframecv.testing.getTestString
import reframecv.ui.components.projects.editor.TestEditorComponent

private const val PROJECT_NAME = "Test Project"
private const val UPDATED_PROJECT_NAME = "Updated Test Project"

@OptIn(ExperimentalTestApi::class)
class EditorUpdateTest {
    private val projectNameLabel = getTestString(Res.string.project_name)
    private val saveLabel = getTestString(Res.string.action_update)
    private val editTitle = getTestString(Res.string.project_editor_edit_title)

    @Test
    fun updatesProjectNameOnEnter() = runComposeUiTest {
        var savedName: String? = null
        setContent {
            EditorContent(TestEditorComponent(initialName = PROJECT_NAME), onSave = {
                savedName = it
            })
        }
        onNodeWithText(projectNameLabel).performTextReplacement("  $UPDATED_PROJECT_NAME  ")
        onNodeWithText(projectNameLabel).performKeyInput { pressKey(Key.Enter) }
        assertEquals(UPDATED_PROJECT_NAME, savedName)
    }

    @Test
    fun editsExistingProjectName() = runComposeUiTest {
        var savedName: String? = null
        setContent {
            EditorContent(
                TestEditorComponent(),
                initialName = PROJECT_NAME,
                onSave = { savedName = it },
            )
        }

        onNodeWithText(editTitle).assertIsDisplayed()
        onNodeWithText(PROJECT_NAME).assertIsDisplayed()
        onNodeWithText(projectNameLabel).performTextReplacement(UPDATED_PROJECT_NAME)
        onNodeWithText(saveLabel).performClick()
        assertEquals(UPDATED_PROJECT_NAME, savedName)
    }
}
