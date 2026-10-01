package ankideckbuilder.ui.compose.projects

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import ankideckbuilder.shared.generated.resources.Res
import ankideckbuilder.shared.generated.resources.action_cancel
import ankideckbuilder.shared.generated.resources.project_editor_create_title
import ankideckbuilder.shared.generated.resources.projects_add
import ankideckbuilder.testing.getTestString
import ankideckbuilder.ui.components.projects.TestProjectsComponent
import kotlin.test.Test

class ProjectsContentTest {
    private val createTitle = getTestString(Res.string.project_editor_create_title)
    private val addProjectLabel = getTestString(Res.string.projects_add)
    private val cancelLabel = getTestString(Res.string.action_cancel)

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
