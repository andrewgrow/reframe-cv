package reframecv.ui.compose.projects

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.asAwtTransferable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import java.awt.datatransfer.DataFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.domain.models.project.Project
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class ProjectListTest {
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun draggingNameSelectsAndCopiesTextWithoutOpeningProject() = runComposeUiTest {
        val project = Project(id = 1, name = "Android Developer", createdAt = 0, updatedAt = 0)
        var opened = 0
        var copied: ClipEntry? = null
        val clipboard = object : Clipboard {
            override val nativeClipboard = java.awt.datatransfer.Clipboard("test")
            override suspend fun getClipEntry(): ClipEntry? = copied
            override suspend fun setClipEntry(clipEntry: ClipEntry?) {
                copied = clipEntry
            }
        }
        setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                ReframeTheme {
                    ProjectList(listOf(project), onEditProject = {}, onOpenProject = { opened++ })
                }
            }
        }
        onNodeWithText(project.name).performMouseInput {
            moveTo(Offset(1f, centerY))
            press()
            moveTo(Offset(150f, centerY), delayMillis = 100)
            release()
        }
        onNodeWithText(project.name).performKeyInput { pressKey(Key.Copy) }
        runOnIdle {
            val text = copied?.asAwtTransferable?.getTransferData(
                DataFlavor.stringFlavor,
            ) as? String
            assertTrue(
                text?.contains("Android") == true,
                "Expected selected project name to be copied",
            )
            assertEquals(0, opened)
        }
        onNodeWithText(project.name).performMouseInput { click(Offset(30f, centerY)) }
        runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun nameOpensProjectWhileEditRemainsSeparate() = runComposeUiTest {
        val project = Project(id = 1, name = "Android Developer", createdAt = 0, updatedAt = 0)
        var edits = 0
        var opened: Project? = null
        setContent {
            ReframeTheme {
                ProjectList(listOf(project), onEditProject = { edits++ }, onOpenProject = {
                    opened =
                        it
                })
            }
        }
        onNodeWithText("Edit").assertDoesNotExist()
        onNodeWithTag(projectRowTag(project.id)).performMouseInput { enter(center) }
        onNodeWithText("Edit").assertIsDisplayed()
        onNodeWithTag(projectRowTag(project.id)).performMouseInput { exit() }
        onNodeWithText("Edit").assertDoesNotExist()
        onNodeWithText(project.name).performClick()
        assertEquals(project, opened)
        assertEquals(0, edits)
        onNodeWithTag(projectRowTag(project.id)).performMouseInput { enter(center) }
        onNodeWithText("Edit").assertIsDisplayed().performClick()
        assertEquals(1, edits)
    }

    @Test
    fun rowFocusRevealsEditAndEditFocusKeepsItVisible() = runComposeUiTest {
        val project = Project(id = 1, name = "Backend", createdAt = 0, updatedAt = 0)
        var edits = 0
        setContent {
            ReframeTheme { ProjectList(listOf(project), onEditProject = { edits++ }) }
        }
        onNodeWithTag(projectRowTag(project.id))
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Edit").assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Edit").assertIsDisplayed().performClick()
        assertEquals(1, edits)
    }
}
