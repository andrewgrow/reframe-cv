package reframecv.ui.compose.projects

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.time.Instant
import reframecv.domain.models.project.Project
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.projects_help_show
import reframecv.testing.GoldenScreenshotTest
import reframecv.testing.getTestString
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.UiState
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ProjectsContentScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun savedProjectsMatchesReference() = runComposeUiTest {
        val now = Instant.fromEpochMilliseconds(1_000L)
        val names = listOf("Android Developer", "Backend", "Google")
        val projects = names.mapIndexed { index, name ->
            Project(id = index.toLong() + 1, name = name, createdAt = now, updatedAt = now)
        }
        captureProjectsGolden(
            this,
            TestProjectsComponent(initialState = UiState.Projects(projects)),
        )
    }

    @Test
    fun expandedProjectHelpInDarkThemeMatchesReference() = runComposeUiTest {
        val component = TestProjectsComponent()
        setGoldenContent(this, themeMode = ThemeMode.Dark) { ProjectsContent(component) }
        onNodeWithContentDescription(getTestString(Res.string.projects_help_show)).performClick()
        captureGolden(this, PROJECTS_SCREEN_TAG)
    }

    @Test
    fun expandedProjectHelpMatchesReference() = runComposeUiTest {
        val component = TestProjectsComponent()
        setGoldenContent(this, themeMode = ThemeMode.Light) { ProjectsContent(component) }
        onNodeWithContentDescription(getTestString(Res.string.projects_help_show)).performClick()
        captureGolden(this, PROJECTS_SCREEN_TAG)
    }

    @Test
    fun openProjectEditorMatchesReference() = runDesktopComposeUiTest {
        val component = TestProjectsComponent(editorInitiallyOpen = true)
        captureProjectsGolden(this, component)
    }

    @Test
    fun emptyProjectsStateMatchesReference() = runComposeUiTest {
        val component = TestProjectsComponent()
        captureProjectsGolden(this, component)
    }

    private fun captureProjectsGolden(composeUiTest: ComposeUiTest, component: ProjectsComponent) {
        setAndCaptureGolden(
            composeUiTest = composeUiTest,
            captureTag = PROJECTS_SCREEN_TAG,
        ) {
            ProjectsContent(component)
        }
    }
}
