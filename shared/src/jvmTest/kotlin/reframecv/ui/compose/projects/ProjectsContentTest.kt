package reframecv.ui.compose.projects

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.project.Project
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.action_edit
import reframecv.shared.generated.resources.action_update
import reframecv.shared.generated.resources.project_add
import reframecv.shared.generated.resources.project_delete_warning
import reframecv.shared.generated.resources.project_editor_create_title
import reframecv.shared.generated.resources.project_editor_edit_title
import reframecv.shared.generated.resources.project_empty
import reframecv.shared.generated.resources.project_name
import reframecv.shared.generated.resources.projects_add
import reframecv.shared.generated.resources.projects_help
import reframecv.shared.generated.resources.projects_help_hide
import reframecv.shared.generated.resources.projects_help_show
import reframecv.testing.getTestString
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.UiState
import reframecv.ui.compose.projects.list.PROJECTS_LIST_TAG
import reframecv.ui.compose.projects.list.projectRowTag

class ProjectsContentTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyChildProjectAllowsCreationWithoutRepeatingItsName() = runComposeUiTest {
        val component = TestProjectsComponent(
            projectPath = listOf(ProjectBreadcrumb(1, "Backend"), ProjectBreadcrumb(2, "Google")),
        )
        setContent { ProjectsContent(component) }
        onNodeWithText(getTestString(Res.string.project_empty)).assertIsDisplayed()
        onNodeWithText("Google").assertDoesNotExist()
        onNodeWithText("Backend").assertDoesNotExist()
        onNodeWithText(getTestString(Res.string.project_add)).performClick()
        onNodeWithText(createTitle).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun projectListScrollsWhileAddButtonStaysFixed() = runComposeUiTest {
        val projects = List(100) { index ->
            Project(id = index.toLong(), name = "Project $index", createdAt = 0, updatedAt = 0)
        }
        setContent {
            ProjectsContent(
                TestProjectsComponent(
                    initialState = UiState.Projects(projects),
                    projectPath = listOf(
                        ProjectBreadcrumb(id = 1, name = "Parent Project"),
                        ProjectBreadcrumb(id = 2, name = "This Project"),
                    ),
                ),
            )
        }
        val addButton = onNodeWithText(addProjectLabel)
        val initialButtonBounds = addButton.fetchSemanticsNode().boundsInRoot
        onNodeWithTag(PROJECTS_LIST_TAG).performScrollToIndex(projects.lastIndex)

        onNodeWithText(projects.last().name).assertIsDisplayed()
        addButton.assertIsDisplayed()
        assertEquals(initialButtonBounds, addButton.fetchSemanticsNode().boundsInRoot)
        addButton.performClick()
        onNodeWithText(createTitle).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deepProjectPathDoesNotRepeatNamesAboveContent() = runComposeUiTest {
        val project = Project(id = 1, name = "Child Project", createdAt = 0, updatedAt = 0)
        setContent {
            ProjectsContent(
                TestProjectsComponent(
                    initialState = UiState.Projects(listOf(project)),
                    projectPath =
                        List(10) {
                            ProjectBreadcrumb(it.toLong(), "Parent project with a long name $it")
                        } + ProjectBreadcrumb(id = 10, name = "This Project"),
                ),
            )
        }
        onNodeWithText("This Project").assertDoesNotExist()
        onNodeWithText("Parent project with a long name 0").assertDoesNotExist()
        onNodeWithText(project.name).assertIsDisplayed()
        onNodeWithText(addProjectLabel).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun opensUpdateDialogFromEditActionAndCancelsDeletion() = runComposeUiTest {
        val now = 1_000L
        val project = Project(id = 1, name = "Android Developer", createdAt = now, updatedAt = now)
        val component = TestProjectsComponent(initialState = UiState.Projects(listOf(project)))
        setContent { ProjectsContent(component) }

        onNodeWithTag(projectRowTag(project.id)).performMouseInput { enter(center) }
        onNodeWithText(getTestString(Res.string.action_edit)).performClick()
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
        val add = onNodeWithText(addProjectLabel)
        val actionBounds = add.fetchSemanticsNode().boundsInRoot

        onNodeWithText(help).assertDoesNotExist()
        onNodeWithContentDescription(getTestString(Res.string.projects_help_show)).performClick()
        onNodeWithText(help).assertIsDisplayed()
        onNodeWithText(addProjectLabel).assertIsDisplayed()
        assertEquals(actionBounds, add.fetchSemanticsNode().boundsInRoot)
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
