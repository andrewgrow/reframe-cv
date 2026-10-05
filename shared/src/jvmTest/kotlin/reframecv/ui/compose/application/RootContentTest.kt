package reframecv.ui.compose.application

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_projects
import reframecv.shared.generated.resources.projects_empty
import reframecv.testing.getTestString
import reframecv.ui.components.application.TestRootComponent

@OptIn(ExperimentalTestApi::class)
class RootContentTest {
    @Test
    fun projectsNavigationRemainsEnabledAndKeepsCurrentScreenVisible() = runComposeUiTest {
        var clicks = 0
        setContent { RootContent(TestRootComponent(onProjectsListClick = { clicks++ })) }
        val navigationLabel = getTestString(Res.string.navigation_projects)
        val emptyMessage = getTestString(Res.string.projects_empty)

        onNodeWithText(navigationLabel).assertIsDisplayed().assertIsEnabled().performClick()
        onNodeWithText(emptyMessage).assertIsDisplayed()
        onNodeWithText(navigationLabel).assertIsEnabled().performClick()
        assertEquals(2, clicks)
        onNodeWithText(emptyMessage).assertIsDisplayed()
    }
}
