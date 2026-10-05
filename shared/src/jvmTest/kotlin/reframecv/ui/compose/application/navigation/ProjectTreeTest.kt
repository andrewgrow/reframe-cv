package reframecv.ui.compose.application.navigation

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.project.Project
import reframecv.ui.components.application.navigation.ProjectTreeState
import reframecv.ui.components.application.navigation.TestProjectTreeComponent
import reframecv.ui.compose.application.RootNavigation
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class ProjectTreeTest {
    private val projects = listOf(
        Project(1, "Backend", 0, 0),
        Project(2, "Java", 0, 0, parentId = 1),
    )

    @Test
    fun expandingDoesNotNavigateAndNameClicksSelectProject() = runComposeUiTest {
        val selected = mutableListOf<Long>()
        val component = TestProjectTreeComponent(
            ProjectTreeState(projects, loading = false),
            onSelect = { selected += it },
        )
        setContent { ReframeTheme { RootNavigation(component) } }
        onNodeWithText("Java").assertDoesNotExist()
        onNodeWithContentDescription("Expand Backend").performClick()
        onNodeWithText("Java").assertIsDisplayed()
        assertEquals(emptyList(), selected)
        onNodeWithText("Java").performClick().assertIsSelected()
        assertEquals(listOf(2L), selected)
        onNodeWithContentDescription("Collapse Backend").performClick()
        onNodeWithText("Java").assertDoesNotExist()
        onNodeWithText("Projects").performClick().assertIsSelected()
    }

    @Test
    fun keyboardExpandsAndOpensWithoutConflatingActions() = runComposeUiTest {
        val selected = mutableListOf<Long>()
        val component = TestProjectTreeComponent(
            ProjectTreeState(projects, loading = false),
            onSelect = { selected += it },
        )
        setContent { ReframeTheme { RootNavigation(component) } }
        onNodeWithText("Backend").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Backend").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionRight)
        }
        onNodeWithText("Java").assertIsDisplayed()
        assertEquals(emptyList(), selected)
        onNodeWithText("Backend").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithText("Java").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(2L), selected)
    }

    @Test
    fun revealsSelectionAfterChangingProjectsInLongTree() = runComposeUiTest {
        val many = List(40) { index -> Project(index.toLong() + 1, "Project $index", 0, 0) }
        val component = TestProjectTreeComponent(ProjectTreeState(many, loading = false))
        setContent { ReframeTheme { RootNavigation(component) } }
        runOnIdle { component.state.value = component.state.value.copy(selectedId = 40) }
        waitForIdle()
        onNodeWithText("Project 39").assertIsDisplayed().assertIsSelected()
        runOnIdle { component.state.value = component.state.value.copy(selectedId = null) }
        waitForIdle()
        onNodeWithText("Projects").assertIsDisplayed().assertIsSelected()
    }
}
