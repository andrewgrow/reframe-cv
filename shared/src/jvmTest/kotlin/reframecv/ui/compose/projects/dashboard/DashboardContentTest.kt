package reframecv.ui.compose.projects.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.project_configure
import reframecv.testing.getTestString
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.dashboard.DashboardState
import reframecv.ui.components.projects.dashboard.TestDashboardComponent
import reframecv.ui.compose.common.LOADING_INDICATOR_TAG
import reframecv.ui.compose.projects.ProjectsContent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class DashboardContentTest {
    @Test
    fun configureInvokesTheProjectCallback() = runComposeUiTest {
        var calls = 0
        val component = TestProjectsComponent(
            projectPath = listOf(ProjectBreadcrumb(1, "Backend")),
            onConfigureProjectClick = { calls++ },
        )
        setContent { ReframeTheme { ProjectsContent(component) } }
        onNodeWithText(getTestString(Res.string.project_configure)).performClick()
        assertEquals(1, calls)
    }

    @Test
    fun narrowDashboardKeepsAllThreeSectionsAndVacanciesActionReachable() = runComposeUiTest {
        val component = TestDashboardComponent()
        setContent {
            ReframeTheme {
                Box(Modifier.size(320.dp, 600.dp)) { DashboardContent(component) }
            }
        }
        onNodeWithText("Resumes").assertIsDisplayed()
        onNodeWithText("Cover letters").performScrollTo().assertIsDisplayed()
        onNodeWithText("Vacancies").performScrollTo().assertIsDisplayed()
        onNodeWithText("Resumes").performScrollTo().assertIsDisplayed()
        onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    @Test
    fun sectionsShowActualCountsAndScrollTheirRecords() = runComposeUiTest {
        val sample = dashboardSample()
        val component = TestDashboardComponent(
            initialState = sample.copy(
                resumes = List(30) {
                    sample.resumes.first().copy(id = it.toLong(), name = "Resume $it")
                },
            ),
        )
        setContent { ReframeTheme { DashboardContent(component) } }
        onNodeWithText("30").assertIsDisplayed()
        onNodeWithText("2").assertIsDisplayed()
        onNodeWithText("1").assertIsDisplayed()
        onNodeWithTag("dashboard.records.Resumes").performScrollToNode(hasText("Resume 29"))
        onNodeWithText("Resume 29").assertIsDisplayed()
        onNodeWithText("Kotlin Developer").assertIsDisplayed()
        runOnIdle { component.uiState.value = sample }
        onNodeWithText("30").assertDoesNotExist()
        onNodeWithText("3").assertIsDisplayed()
        onNodeWithText("Android Developer").assertIsDisplayed()
    }

    @Test
    fun loadingDelaysSpinnerAndFailureOffersRetryWithoutZeroCounts() = runComposeUiTest {
        mainClock.autoAdvance = false
        var retries = 0
        val component =
            TestDashboardComponent(initialState = DashboardState.Loading, retry = { retries++ })
        setContent { ReframeTheme { DashboardContent(component) } }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(900)
        onNodeWithText("0").assertDoesNotExist()
        onNodeWithTag(LOADING_INDICATOR_TAG).assertDoesNotExist()
        mainClock.advanceTimeBy(200)
        onNodeWithTag(LOADING_INDICATOR_TAG).assertIsDisplayed()
        runOnIdle { component.uiState.value = DashboardState.LoadFailed }
        mainClock.advanceTimeByFrame()
        onNodeWithTag(LOADING_INDICATOR_TAG).assertDoesNotExist()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
        runOnIdle { component.uiState.value = DashboardState.Ready() }
        mainClock.advanceTimeByFrame()
        onAllNodes(androidx.compose.ui.test.hasText("0")).assertCountEquals(3)
    }

    @Test
    fun entireVacanciesSectionInvokesNavigation() = runComposeUiTest {
        var opened = 0
        val component = TestDashboardComponent(
            initialState = dashboardSample(),
            openVacancies = { opened++ },
        )
        setContent { ReframeTheme { DashboardContent(component) } }
        onNodeWithText("View all").assertDoesNotExist()
        onNodeWithText("Vacancies").performClick()
        onNodeWithText("Kotlin Developer", useUnmergedTree = true).performMouseInput { click() }
        onNodeWithTag("dashboard.section.Vacancies").performMouseInput {
            click(bottomCenter - androidx.compose.ui.geometry.Offset(0f, 10f))
        }
        assertEquals(3, opened)
    }
}
