package reframecv.ui.compose.projects

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.domain.models.project.Project
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.action_update
import reframecv.shared.generated.resources.project_delete_warning
import reframecv.shared.generated.resources.project_editor_create_title
import reframecv.shared.generated.resources.project_editor_edit_title
import reframecv.shared.generated.resources.project_name
import reframecv.shared.generated.resources.projects_add
import reframecv.shared.generated.resources.projects_help
import reframecv.shared.generated.resources.projects_help_hide
import reframecv.shared.generated.resources.projects_help_show
import reframecv.testing.getTestString
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.UiState

class ProjectsContentTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun opensUpdateDialogFromProjectNameAndCancelsDeletion() = runComposeUiTest {
        val now = 1_000L
        val project = Project(id = 1, name = "Android Developer", createdAt = now, updatedAt = now)
        val component = TestProjectsComponent(initialState = UiState.Projects(listOf(project)))
        setContent { ProjectsContent(component) }

        onNodeWithText(project.name).performClick()
        onNodeWithText(getTestString(Res.string.project_editor_edit_title)).assertIsDisplayed()
        onNodeWithText(getTestString(Res.string.project_name)).assertTextContains(project.name)
        onNodeWithText(getTestString(Res.string.action_update)).assertIsDisplayed()
        onNodeWithText(getTestString(Res.string.action_delete)).performClick()
        onNodeWithText(getTestString(Res.string.project_delete_warning)).assertIsDisplayed()
        onNodeWithText(getTestString(Res.string.project_editor_edit_title)).assertIsDisplayed()
        onNodeWithText(cancelLabel).performClick()
        onNodeWithText(getTestString(Res.string.project_delete_warning)).assertDoesNotExist()
        onNodeWithText(getTestString(Res.string.project_editor_edit_title)).assertIsDisplayed()
        onNodeWithText(cancelLabel).performClick()
        onNodeWithText(project.name).assertIsDisplayed()
    }

    private val createTitle = getTestString(Res.string.project_editor_create_title)
    private val addProjectLabel = getTestString(Res.string.projects_add)
    private val cancelLabel = getTestString(Res.string.action_cancel)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun displaysSavedProjectsAndOpensCreationDialog() = runComposeUiTest {
        val now = 1_000L
        val project = Project(id = 1, name = "Android Developer", createdAt = now, updatedAt = now)
        setContent {
            ProjectsContent(TestProjectsComponent(initialState = UiState.Projects(listOf(project))))
        }
        onNodeWithText(project.name).assertIsDisplayed()
        onNodeWithText(addProjectLabel).performClick()
        onNodeWithText(createTitle).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun projectHelpIsHiddenInitiallyAndTogglesOnClick() = runComposeUiTest {
        val help = getTestString(Res.string.projects_help)
        setContent { ProjectsContent(TestProjectsComponent()) }

        onNodeWithText(help).assertDoesNotExist()
        onNodeWithContentDescription(getTestString(Res.string.projects_help_show)).performClick()
        onNodeWithText(help).assertIsDisplayed()
        onNodeWithText(addProjectLabel).assertIsDisplayed()
        onNodeWithContentDescription(getTestString(Res.string.projects_help_hide)).performClick()
        onNodeWithText(help).assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun displaysAndClosesEditorFromSlot() = runComposeUiTest {
        val component = TestProjectsComponent()
        setContent { ProjectsContent(component) }

        onNodeWithText(createTitle).assertDoesNotExist()
        onNodeWithText(addProjectLabel).performClick()
        onNodeWithText(createTitle).assertIsDisplayed()
        onNodeWithText(cancelLabel).performClick()
        onNodeWithText(createTitle).assertDoesNotExist()
        onNodeWithText(addProjectLabel).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun closesInitiallyOpenEditor() = runComposeUiTest {
        val component = TestProjectsComponent(editorInitiallyOpen = true)
        setContent { ProjectsContent(component) }

        onNodeWithText(createTitle).assertIsDisplayed()
        onNodeWithText(cancelLabel).performClick()
        onNodeWithText(createTitle).assertDoesNotExist()
        onNodeWithText(addProjectLabel).performClick()
        onNodeWithText(createTitle).assertIsDisplayed()
    }
}
