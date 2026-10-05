package reframecv.ui.compose.projects.empty

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.project.Project
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.project_add
import reframecv.shared.generated.resources.project_configure
import reframecv.shared.generated.resources.project_editor_create_title
import reframecv.shared.generated.resources.project_empty
import reframecv.shared.generated.resources.project_empty_help
import reframecv.shared.generated.resources.project_empty_help_example
import reframecv.shared.generated.resources.project_empty_help_footer
import reframecv.shared.generated.resources.projects_help_hide
import reframecv.shared.generated.resources.projects_help_show
import reframecv.testing.getTestString
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.UiState
import reframecv.ui.compose.projects.ProjectsContent

@OptIn(ExperimentalTestApi::class)
class EmptyProjectContentTest {
    @Test
    fun hoveringConfigureClearsAddFocusAfterCancellingDialog() = runComposeUiTest {
        val component = TestProjectsComponent(projectPath = listOf(ProjectBreadcrumb(1, "Backend")))
        setContent { ProjectsContent(component) }
        val add = onNodeWithText(getTestString(Res.string.project_add))
        val configure = onNodeWithText(getTestString(Res.string.project_configure))
        add.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        add.performClick()
        onNodeWithText(getTestString(Res.string.action_cancel)).performClick()
        onNodeWithText(getTestString(Res.string.project_editor_create_title)).assertDoesNotExist()
        // The headless host does not restore native window focus after closing a dialog.
        add.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        add.assertIsFocused()
        configure.performMouseInput { enter(center) }
        add.assertIsNotFocused()
        configure.assertIsNotFocused()
    }

    @Test
    fun rootProjectOffersConfigurationAndCreationAndTogglesHelp() = runComposeUiTest {
        val component = TestProjectsComponent(projectPath = listOf(ProjectBreadcrumb(1, "Backend")))
        val message = getTestString(Res.string.project_empty)
        val help = getTestString(Res.string.project_empty_help)
        val example = getTestString(Res.string.project_empty_help_example)
        val footer = getTestString(Res.string.project_empty_help_footer)
        val title = getTestString(Res.string.project_editor_create_title)
        setContent { ProjectsContent(component) }
        val add = onNodeWithText(getTestString(Res.string.project_add))
        val actionBounds = add.fetchSemanticsNode().boundsInRoot
        onNodeWithText(message).assertIsDisplayed()
        onNodeWithText(help).assertDoesNotExist()
        onNodeWithText(getTestString(Res.string.project_configure)).performClick()
        onNodeWithText(title).assertDoesNotExist()
        onNodeWithText(message).assertIsDisplayed()
        onNodeWithContentDescription(getTestString(Res.string.projects_help_show)).performClick()
        onNodeWithText(help).assertIsDisplayed()
        onNodeWithText(example).assertIsDisplayed()
        onNodeWithText(footer).assertIsDisplayed()
        assertEquals(actionBounds, add.fetchSemanticsNode().boundsInRoot)
        onNodeWithContentDescription(getTestString(Res.string.projects_help_hide)).performClick()
        onNodeWithText(help).assertDoesNotExist()
        onNodeWithText(example).assertDoesNotExist()
        onNodeWithText(getTestString(Res.string.project_add)).performClick()
        onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun firstChildReplacesPlaceholderWithProjectList() = runComposeUiTest {
        val component = TestProjectsComponent(projectPath = listOf(ProjectBreadcrumb(1, "Backend")))
        setContent { ProjectsContent(component) }
        onNodeWithText(getTestString(Res.string.project_empty)).assertIsDisplayed()
        onNodeWithContentDescription(getTestString(Res.string.projects_help_show)).performClick()
        runOnIdle {
            component.uiState.value = UiState.Projects(
                listOf(Project(id = 2, name = "Java", createdAt = 0, updatedAt = 0, parentId = 1)),
            )
        }
        onNodeWithText("Java").assertIsDisplayed()
        onNodeWithText(getTestString(Res.string.project_empty)).assertDoesNotExist()
        onNodeWithText(getTestString(Res.string.project_configure)).assertDoesNotExist()
        onNodeWithText(getTestString(Res.string.project_empty_help)).assertDoesNotExist()
    }
}
