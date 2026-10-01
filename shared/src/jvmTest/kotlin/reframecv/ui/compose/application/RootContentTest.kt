package reframecv.ui.compose.application

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.projects_add
import reframecv.shared.generated.resources.projects_empty
import reframecv.testing.getTestString
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.projects.TestProjectsComponent

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
