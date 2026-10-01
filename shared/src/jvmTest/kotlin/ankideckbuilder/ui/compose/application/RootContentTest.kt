package ankideckbuilder.ui.compose.application

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import ankideckbuilder.shared.generated.resources.Res
import ankideckbuilder.shared.generated.resources.projects_add
import ankideckbuilder.shared.generated.resources.projects_empty
import ankideckbuilder.testing.getTestString
import ankideckbuilder.ui.components.application.TestRootComponent
import ankideckbuilder.ui.components.projects.TestProjectsComponent
import kotlin.test.Test
import kotlin.test.assertEquals

class RootContentTest {
    private val emptyMessage = getTestString(Res.string.projects_empty)
    private val addProjectLabel = getTestString(Res.string.projects_add)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun displaysProjectsAndDeliversAddProjectClick() = runComposeUiTest {
        var clicks = 0
        val projects = TestProjectsComponent(onAddProjectClick = { clicks++ })
        val root = TestRootComponent(projects)
        setContent { RootContent(root) }
        onNodeWithText(emptyMessage).assertIsDisplayed()
        onNodeWithText(addProjectLabel).assertIsDisplayed()
        onNodeWithText(addProjectLabel).performClick()
        runOnIdle { assertEquals(1, clicks) }
    }
}
