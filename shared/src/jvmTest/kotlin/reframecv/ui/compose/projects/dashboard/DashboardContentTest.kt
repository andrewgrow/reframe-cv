package reframecv.ui.compose.projects.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.project_configure
import reframecv.testing.getTestString
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.dashboard.DashboardComponent
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
    fun narrowDashboardKeepsAllThreeSectionsReachableWithoutActions() = runComposeUiTest {
        val component = object : DashboardComponent {
            override val projectId = 1L
        }
        setContent {
            ReframeTheme {
                Box(Modifier.size(320.dp, 600.dp)) { DashboardContent(component) }
            }
        }
        onNodeWithText("Resumes").assertIsDisplayed()
        onNodeWithText("Cover letters").performScrollTo().assertIsDisplayed()
        onNodeWithText("Vacancies").performScrollTo().assertIsDisplayed()
        onNodeWithText("Resumes").performScrollTo().assertIsDisplayed()
        onAllNodes(hasClickAction()).assertCountEquals(0)
    }
}
