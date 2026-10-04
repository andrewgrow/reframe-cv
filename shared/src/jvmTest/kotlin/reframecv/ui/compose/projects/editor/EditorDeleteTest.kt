package reframecv.ui.compose.projects.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.action_full_delete
import reframecv.shared.generated.resources.action_update
import reframecv.shared.generated.resources.project_delete_confirmation
import reframecv.shared.generated.resources.project_delete_error
import reframecv.shared.generated.resources.project_delete_warning
import reframecv.shared.generated.resources.project_editor_edit_title
import reframecv.shared.generated.resources.project_name
import reframecv.testing.getTestString
import reframecv.ui.components.projects.editor.EditorDeleteState
import reframecv.ui.components.projects.editor.TestEditorComponent

private const val PROJECT_NAME = "Test Project"
private const val UPDATED_PROJECT_NAME = "Updated Test Project"

@OptIn(ExperimentalTestApi::class)
class EditorDeleteTest {
    private val projectNameLabel = getTestString(Res.string.project_name)
    private val saveLabel = getTestString(Res.string.action_update)
    private val editTitle = getTestString(Res.string.project_editor_edit_title)

    @Test
    fun confirmsDeletionOnEnterIgnoringCase() = runComposeUiTest {
        val confirmations = mutableListOf<String>()
        val component =
            TestEditorComponent(initialName = PROJECT_NAME, onDeleteClick = confirmations::add)
        val confirmationLabel = getTestString(Res.string.project_delete_confirmation)
        val fullDeleteLabel = getTestString(Res.string.action_full_delete)
        setContent { EditorContent(component) }
        onNodeWithText(getTestString(Res.string.action_delete)).performClick()
        onNodeWithText(confirmationLabel).performTextReplacement("DELET")
        onNodeWithText(confirmationLabel).performKeyInput { pressKey(Key.Enter) }
        assertEquals(emptyList(), confirmations)
        listOf("delete", "Delete", "dElEtE", "DELETE").forEach {
            onNodeWithText(confirmationLabel).performTextReplacement(it)
            onNodeWithText(fullDeleteLabel).assertIsEnabled()
            onNodeWithText(confirmationLabel).performKeyInput { pressKey(Key.Enter) }
        }
        assertEquals(listOf("delete", "Delete", "dElEtE", "DELETE"), confirmations)
    }

    @Test
    fun cancelsDeletionBeforeClosingEditorAndPreservesEditedName() = runComposeUiTest {
        var closed = false
        val component = TestEditorComponent(initialName = PROJECT_NAME) { closed = true }
        val cancelLabel = getTestString(Res.string.action_cancel)
        val warning = getTestString(Res.string.project_delete_warning)
        setContent { EditorContent(component) }

        onNodeWithText(projectNameLabel).performTextReplacement(UPDATED_PROJECT_NAME)
        onNodeWithText(getTestString(Res.string.action_delete)).performClick()
        onNodeWithText(warning).assertIsDisplayed()
        onNodeWithText(getTestString(Res.string.action_delete)).assertDoesNotExist()
        onNodeWithText(saveLabel).assertDoesNotExist()
        onNodeWithText(cancelLabel).performClick()

        onNodeWithText(warning).assertDoesNotExist()
        onNodeWithText(getTestString(Res.string.action_full_delete)).assertDoesNotExist()
        onNodeWithText(editTitle).assertIsDisplayed()
        onNodeWithText(UPDATED_PROJECT_NAME).assertIsDisplayed()
        onNodeWithText(getTestString(Res.string.action_delete)).assertIsDisplayed()
        onNodeWithText(saveLabel).assertIsEnabled()
        assertFalse(closed)
        onNodeWithText(cancelLabel).performClick()
        assertTrue(closed)
    }

    @Test
    fun requiresDeleteWordAndPreservesInputOnFailure() = runComposeUiTest {
        var confirmation: String? = null
        lateinit var component: TestEditorComponent
        component = TestEditorComponent(
            initialName = PROJECT_NAME,
            onDeleteClick = {
                confirmation = it
                component.deleteState.value = EditorDeleteState.Failed
            },
        )
        val deleteLabel = getTestString(Res.string.action_delete)
        val fullDeleteLabel = getTestString(Res.string.action_full_delete)
        val confirmationLabel = getTestString(Res.string.project_delete_confirmation)
        setContent { EditorContent(component) }
        onNodeWithText(fullDeleteLabel).assertDoesNotExist()
        onNodeWithText(deleteLabel).performClick()
        onNodeWithText(getTestString(Res.string.project_delete_warning)).assertIsDisplayed()
        onNodeWithText(confirmationLabel).assertIsFocused()
        onNodeWithText(fullDeleteLabel).assertIsNotEnabled()
        listOf("DELET", " DELETE", "DELETE ").forEach {
            onNodeWithText(confirmationLabel).performTextReplacement(it)
            onNodeWithText(fullDeleteLabel).assertIsNotEnabled()
        }
        onNodeWithText(confirmationLabel).performTextReplacement("DELETE")
        onNodeWithText(fullDeleteLabel).assertIsEnabled().performClick()
        assertEquals("DELETE", confirmation)
        onNodeWithText(getTestString(Res.string.project_delete_error)).assertIsDisplayed()
        onNodeWithText(PROJECT_NAME).assertIsDisplayed()
        onNodeWithText("DELETE").assertIsDisplayed()
        onNodeWithText(fullDeleteLabel).assertIsEnabled()
        runOnIdle { component.deleteState.value = EditorDeleteState.Deleting }
        onNodeWithText(fullDeleteLabel).assertIsNotEnabled()
        onNodeWithText(deleteLabel).assertDoesNotExist()
        onNodeWithText(saveLabel).assertDoesNotExist()
        onNodeWithText(getTestString(Res.string.action_cancel)).assertIsNotEnabled()
        onNodeWithText(confirmationLabel).assertIsNotEnabled()
        onNodeWithText(projectNameLabel).assertIsNotEnabled()
    }
}
