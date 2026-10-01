package ankideckbuilder.ui.compose.projects.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import ankideckbuilder.shared.generated.resources.Res
import ankideckbuilder.shared.generated.resources.action_create
import ankideckbuilder.shared.generated.resources.action_save
import ankideckbuilder.shared.generated.resources.project_editor_edit_title
import ankideckbuilder.shared.generated.resources.project_name
import ankideckbuilder.testing.getTestString
import ankideckbuilder.ui.components.projects.editor.TestEditorComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val PROJECT_NAME = "Test Project"
private const val UPDATED_PROJECT_NAME = "Updated Test Project"

@OptIn(ExperimentalTestApi::class)
class EditorContentTest {
    private val projectNameLabel = getTestString(Res.string.project_name)
    private val createLabel = getTestString(Res.string.action_create)
    private val saveLabel = getTestString(Res.string.action_save)
    private val editTitle = getTestString(Res.string.project_editor_edit_title)

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
        onNodeWithText(createLabel).performClick()
        assertEquals(PROJECT_NAME, savedName)
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
