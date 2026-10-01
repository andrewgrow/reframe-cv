package reframecv.ui.compose.projects

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.project_editor_create_title
import reframecv.shared.generated.resources.projects_add
import reframecv.shared.generated.resources.projects_help
import reframecv.shared.generated.resources.projects_help_hide
import reframecv.shared.generated.resources.projects_help_show
import reframecv.testing.getTestString
import reframecv.ui.components.projects.TestProjectsComponent

class ProjectsContentTest {
    private val createTitle = getTestString(Res.string.project_editor_create_title)
    private val addProjectLabel = getTestString(Res.string.projects_add)
    private val cancelLabel = getTestString(Res.string.action_cancel)

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
