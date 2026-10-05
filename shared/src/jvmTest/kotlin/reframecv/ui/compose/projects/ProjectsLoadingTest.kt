package reframecv.ui.compose.projects

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.domain.models.project.Project
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.project_empty
import reframecv.shared.generated.resources.projects_load_error
import reframecv.testing.getTestString
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.UiState
import reframecv.ui.compose.common.LOADING_INDICATOR_TAG
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class ProjectsLoadingTest {
    @Test
    fun showsSpinnerAfterOneSecondAndRemovesItOnFailure() = runComposeUiTest {
        mainClock.autoAdvance = false
        val component = TestProjectsComponent(
            initialState = UiState.Loading,
            projectPath = listOf(ProjectBreadcrumb(1, "Backend")),
        )
        setContent { ReframeTheme { ProjectsContent(component) } }
        mainClock.advanceTimeByFrame()
        onNodeWithText("Backend").assertDoesNotExist()
        onNodeWithText(getTestString(Res.string.project_empty)).assertDoesNotExist()
        mainClock.advanceTimeBy(900)
        onNodeWithTag(LOADING_INDICATOR_TAG).assertDoesNotExist()
        mainClock.advanceTimeBy(200)
        onNodeWithTag(LOADING_INDICATOR_TAG).assertIsDisplayed()
        runOnIdle { component.uiState.value = UiState.LoadFailed }
        mainClock.advanceTimeByFrame()
        onNodeWithTag(LOADING_INDICATOR_TAG).assertDoesNotExist()
        onNodeWithText(getTestString(Res.string.projects_load_error)).assertIsDisplayed()
        onNodeWithText("Backend").assertDoesNotExist()
    }

    @Test
    fun fastResponseCancelsSpinnerAndEachLoadingStartsANewDelay() = runComposeUiTest {
        mainClock.autoAdvance = false
        val component = TestProjectsComponent(initialState = UiState.Loading)
        setContent { ReframeTheme { ProjectsContent(component) } }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(500)
        runOnIdle {
            component.uiState.value = UiState.Projects(
                listOf(Project(id = 1, name = "Backend", createdAt = 0, updatedAt = 0)),
            )
        }
        mainClock.advanceTimeByFrame()
        onNodeWithText("Backend").assertIsDisplayed()
        mainClock.advanceTimeBy(2_000)
        onNodeWithTag(LOADING_INDICATOR_TAG).assertDoesNotExist()
        runOnIdle { component.uiState.value = UiState.Loading }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(900)
        onNodeWithTag(LOADING_INDICATOR_TAG).assertDoesNotExist()
        mainClock.advanceTimeBy(200)
        onNodeWithTag(LOADING_INDICATOR_TAG).assertIsDisplayed()
        runOnIdle { component.uiState.value = UiState.NoProjects }
        mainClock.advanceTimeByFrame()
        onNodeWithTag(LOADING_INDICATOR_TAG).assertDoesNotExist()
    }
}
