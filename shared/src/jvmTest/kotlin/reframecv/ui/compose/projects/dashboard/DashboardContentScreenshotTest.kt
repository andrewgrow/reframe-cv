package reframecv.ui.compose.projects.dashboard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.domain.models.project.Project
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.application.navigation.ProjectTreeState
import reframecv.ui.components.projects.dashboard.DashboardComponent
import reframecv.ui.compose.application.RootContent
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.theme.ReframeTheme.ThemeMode
import reframecv.ui.theme.UiScale

@OptIn(ExperimentalTestApi::class)
class DashboardContentScreenshotTest : GoldenScreenshotTest() {
    private val component = object : DashboardComponent {
        override val projectId = 1L
    }

    private fun rootComponent() = TestRootComponent(
        dashboardComponent = component,
        initialTreeState = ProjectTreeState(
            projects = listOf(Project(id = 1, name = "Backend", createdAt = 0, updatedAt = 0)),
            selectedId = 1,
            loading = false,
        ),
    )

    @Test
    fun dashboardInDarkThemeMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this) { RootContent(rootComponent()) }
    }

    @Test
    fun dashboardInLightThemeMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this, themeMode = ThemeMode.Light) {
            RootContent(rootComponent())
        }
    }

    @Test
    fun narrowDashboardMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this, screenSize = ScreenSize(320.dp, 600.dp)) {
            DashboardContent(component)
        }
    }

    @Test
    fun increasedScaleMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this) {
            ReframeTheme(themeMode = ThemeMode.Dark, uiScale = UiScale(150)) {
                DashboardContent(component)
            }
        }
    }
}
