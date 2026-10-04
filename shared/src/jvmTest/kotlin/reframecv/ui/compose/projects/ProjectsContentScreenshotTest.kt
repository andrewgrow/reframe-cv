package reframecv.ui.compose.projects

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.domain.models.project.Project
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.projects_add
import reframecv.shared.generated.resources.projects_help_show
import reframecv.shared.generated.resources.projects_load_error
import reframecv.testing.GoldenScreenshotTest
import reframecv.testing.getTestString
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.UiState
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ProjectsContentScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun emptyChildProjectMatchesReference() = runComposeUiTest {
        captureProjectsGolden(
            this,
            TestProjectsComponent(
                projectPath = listOf(
                    ProjectBreadcrumb(1, "Backend"),
                    ProjectBreadcrumb(2, "Google"),
                ),
            ),
        )
    }

    @Test
    fun childProjectsMatchesReference() = runComposeUiTest {
        captureProjectsGolden(
            this,
            TestProjectsComponent(
                projectPath = listOf(ProjectBreadcrumb(1, "Backend")),
                initialState = UiState.Projects(
                    listOf(
                        Project(
                            id = 2,
                            name = "Google",
                            createdAt = 0,
                            updatedAt = 0,
                            parentId = 1,
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun failedProjectsLoadMatchesReference() = runComposeUiTest {
        setGoldenContent(this, themeMode = ThemeMode.Light) {
            ProjectsContent(TestProjectsComponent(initialState = UiState.LoadFailed))
        }
        onNodeWithText(getTestString(Res.string.projects_load_error)).assertIsDisplayed()
        onNodeWithText(getTestString(Res.string.projects_add)).assertDoesNotExist()
        captureGolden(this, PROJECTS_SCREEN_TAG)
    }

    @Test
    fun savedProjectsMatchesReference() = runComposeUiTest {
        val now = 1_000L
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
    fun savedProjectsInLightThemeMatchesReference() = runComposeUiTest {
        val project = Project(id = 1, name = "Android Developer", createdAt = 0, updatedAt = 0)
        setAndCaptureGolden(this, themeMode = ThemeMode.Light, captureTag = PROJECTS_SCREEN_TAG) {
            ProjectsContent(TestProjectsComponent(initialState = UiState.Projects(listOf(project))))
        }
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
