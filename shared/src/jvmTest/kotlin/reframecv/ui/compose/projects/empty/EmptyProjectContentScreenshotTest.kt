package reframecv.ui.compose.projects.empty

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.project_add
import reframecv.shared.generated.resources.project_configure
import reframecv.shared.generated.resources.project_empty
import reframecv.shared.generated.resources.project_empty_help_example
import reframecv.shared.generated.resources.projects_help_show
import reframecv.testing.GoldenScreenshotTest
import reframecv.testing.getTestString
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.compose.projects.PROJECTS_SCREEN_TAG
import reframecv.ui.compose.projects.ProjectsContent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class EmptyProjectContentScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun expandedHelpOnNarrowScreenMatchesReference() {
        val screen = ScreenSize(320.dp, 720.dp)
        runDesktopComposeUiTest(screen) {
            val contentHeight = mutableStateOf(screen.height)
            setGoldenContent(this, screenSize = screen) {
                Box(Modifier.height(contentHeight.value)) {
                    ProjectsContent(
                        TestProjectsComponent(
                            projectPath = listOf(ProjectBreadcrumb(1, "Backend")),
                        ),
                    )
                }
            }
            onNodeWithContentDescription(
                getTestString(Res.string.projects_help_show),
            ).performClick()
            onNodeWithText(getTestString(Res.string.project_empty_help_example)).assertIsDisplayed()
            val configure = onNodeWithText(getTestString(Res.string.project_configure))
            val add = onNodeWithText(getTestString(Res.string.project_add))
            configure.assertIsDisplayed()
            add.assertIsDisplayed()
            val configureBounds = configure.fetchSemanticsNode().boundsInRoot
            val addBounds = add.fetchSemanticsNode().boundsInRoot
            assertTrue(configureBounds.bottom <= addBounds.top, "Buttons should wrap into two rows")
            captureGolden(this, PROJECTS_SCREEN_TAG)

            runOnIdle { contentHeight.value = 320.dp }
            add.assertIsDisplayed()
            configure.assertIsDisplayed()
            onNodeWithText(getTestString(Res.string.project_empty))
                .performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun emptyProjectInLightThemeMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, captureTag = PROJECTS_SCREEN_TAG, themeMode = ThemeMode.Light) {
            ProjectsContent(
                TestProjectsComponent(projectPath = listOf(ProjectBreadcrumb(1, "Backend"))),
            )
        }
    }

    @Test
    fun expandedHelpMatchesReference() = captureHelp(ThemeMode.Dark)

    @Test
    fun expandedHelpInLightThemeMatchesReference() = captureHelp(ThemeMode.Light)

    private fun captureHelp(themeMode: ThemeMode) = runDesktopComposeUiTest {
        setGoldenContent(this, themeMode = themeMode) {
            ProjectsContent(
                TestProjectsComponent(projectPath = listOf(ProjectBreadcrumb(1, "Backend"))),
            )
        }
        onNodeWithContentDescription(getTestString(Res.string.projects_help_show)).performClick()
        captureGolden(this, PROJECTS_SCREEN_TAG)
    }
}
