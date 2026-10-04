package reframecv.ui.compose.projects

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.project.Project
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class ProjectListTest {
    @Test
    fun hoverRevealsEditWhileNameClickDoesNothing() = runComposeUiTest {
        val project = Project(id = 1, name = "Android Developer", createdAt = 0, updatedAt = 0)
        var edits = 0
        setContent {
            ReframeTheme { ProjectList(listOf(project), onEditProject = { edits++ }) }
        }
        onNodeWithText("Edit").assertDoesNotExist()
        onNodeWithText(project.name).assertHasNoClickAction().performMouseInput { click() }
        assertEquals(0, edits)
        onNodeWithTag(projectRowTag(project.id)).performMouseInput { enter(center) }
        onNodeWithText("Edit").assertIsDisplayed()
        onNodeWithTag(projectRowTag(project.id)).performMouseInput { exit() }
        onNodeWithText("Edit").assertDoesNotExist()
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
