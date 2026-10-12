package reframecv.ui.compose.projects.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import reframecv.ui.components.projects.editor.EditorDeleteState
import reframecv.ui.components.projects.editor.EditorSaveState
import reframecv.ui.components.projects.editor.TestEditorComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class EditorDialogResizeTest {
    @Test
    fun resizesBothDimensionsAndReturnsToOriginalMinimum() =
        runDesktopComposeUiTest(width = 1200, height = 1000) {
            var closed = false
            val component = TestEditorComponent(onCloseClick = { closed = true })
            setContent { ReframeTheme { EditorContent(component) } }
            val dialog = onNodeWithTag(PROJECT_EDITOR_TAG)
            val original = dialog.fetchSemanticsNode().size
            val handleSize = onNodeWithTag("dialog.resize").fetchSemanticsNode().size
            assertTrue(handleSize.width <= 32 && handleSize.height <= 32)
            onNodeWithTag("dialog.resize").performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(60f, 60f))
                release()
            }
            val expanded = dialog.fetchSemanticsNode().size
            assertTrue(expanded.width >= original.width + 100)
            assertTrue(expanded.height >= original.height + 100)
            onNodeWithText("Project name").performTextReplacement("Backend")
            assertEquals(expanded, dialog.fetchSemanticsNode().size)
            onNodeWithText("Create").assertExists()
            onNodeWithTag("dialog.resize").performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(-300f, -300f))
                release()
            }
            assertEquals(original, dialog.fetchSemanticsNode().size)
            assertFalse(closed, "Releasing the resize handle must not dismiss the dialog")
        }

    @Test
    fun reachesWindowBoundsAndImmediatelyShrinksOnReverseDrag() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent { ReframeTheme { EditorContent(TestEditorComponent()) } }
            val handle = onNodeWithTag("dialog.resize")
            handle.performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(500f, 500f))
                release()
            }
            val maximum = onNodeWithTag(PROJECT_EDITOR_TAG).fetchSemanticsNode().size
            assertEquals(800, maximum.width)
            assertEquals(600, maximum.height)
            handle.performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(-20f, 0f))
                release()
            }
            assertEquals(760, onNodeWithTag(PROJECT_EDITOR_TAG).fetchSemanticsNode().size.width)
        }

    @Test
    fun confirmationRemainsUsableAfterResizing() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent {
                ReframeTheme { EditorContent(TestEditorComponent(initialName = "Backend")) }
            }
            val dialog = onNodeWithTag(PROJECT_EDITOR_TAG)
            val minimum = dialog.fetchSemanticsNode().size
            onNodeWithTag("dialog.resize").performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(40f, 160f))
                release()
            }
            val expanded = dialog.fetchSemanticsNode().size
            onNodeWithText("Delete").performClick()
            onNodeWithText("Type DELETE to confirm").performTextReplacement("DELETE")
            onNodeWithText("FULL DELETE THIS PROJECT").assertIsDisplayed()
            assertEquals(expanded, dialog.fetchSemanticsNode().size)
            onNodeWithText("Cancel").performClick()
            assertEquals(expanded, dialog.fetchSemanticsNode().size)
            onNodeWithText("Backend").assertIsDisplayed()
            onNodeWithTag("dialog.resize").performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(-500f, -500f))
                release()
            }
            assertEquals(minimum, dialog.fetchSemanticsNode().size)
        }

    @Test
    fun writesDisableResizing() = runDesktopComposeUiTest(width = 800, height = 600) {
        val component = TestEditorComponent(initialName = "Backend")
        setContent { ReframeTheme { EditorContent(component) } }
        runOnIdle { component.saveState.value = EditorSaveState.Saving }
        onNodeWithTag("dialog.resize").assertIsNotEnabled()
        runOnIdle {
            component.saveState.value = EditorSaveState.Idle
            component.deleteState.value = EditorDeleteState.Deleting
        }
        onNodeWithTag("dialog.resize").assertIsNotEnabled()
    }
}
