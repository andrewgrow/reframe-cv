package reframecv.ui.compose.application

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.project.Project
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_projects
import reframecv.shared.generated.resources.projects_empty
import reframecv.testing.getTestString
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.UiState
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.theme.UiScale

@OptIn(ExperimentalTestApi::class)
class RootContentTest {
    @Test
    fun menuAndContentTextAlignAtDefaultScale() = assertAlignedRows(100)

    @Test
    fun menuAndContentTextAlignAtIncreasedScale() = assertAlignedRows(150)

    private fun assertAlignedRows(percent: Int) = runComposeUiTest {
        val project = Project(id = 1, name = "Android Developer", createdAt = 0, updatedAt = 0)
        val component = TestRootComponent(
            projectsComponent = TestProjectsComponent(
                initialState = UiState.Projects(listOf(project)),
            ),
        )
        var menuTextInset = 0f
        setContent {
            ReframeTheme(uiScale = UiScale(percent)) {
                menuTextInset = with(LocalDensity.current) {
                    ReframeTheme.tokens.spacing.small.toPx()
                }
                RootContent(component)
            }
        }
        val menu = onNodeWithText(getTestString(Res.string.navigation_projects))
        val content = onNodeWithText(project.name)
        assertEquals(
            menu.fetchSemanticsNode().boundsInRoot.top + menuTextInset,
            content.fetchSemanticsNode().boundsInRoot.top,
        )
    }

    @Test
    fun projectsNavigationRemainsEnabledAndKeepsCurrentScreenVisible() = runComposeUiTest {
        var clicks = 0
        setContent { RootContent(TestRootComponent(onProjectsListClick = { clicks++ })) }
        val navigationLabel = getTestString(Res.string.navigation_projects)
        val emptyMessage = getTestString(Res.string.projects_empty)

        onNode(
            hasText(navigationLabel) and hasClickAction(),
        ).assertIsDisplayed().assertIsEnabled().performClick()
        onNodeWithText(emptyMessage).assertIsDisplayed()
        onNode(hasText(navigationLabel) and hasClickAction()).assertIsEnabled().performClick()
        assertEquals(2, clicks)
        onNodeWithText(emptyMessage).assertIsDisplayed()
    }
}
