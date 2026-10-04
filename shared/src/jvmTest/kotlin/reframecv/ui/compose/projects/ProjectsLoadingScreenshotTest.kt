package reframecv.ui.compose.projects

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.UiState
import reframecv.ui.compose.common.LOADING_INDICATOR_TAG
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ProjectsLoadingScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun delayedSpinnerMatchesReference() = captureLoading(ThemeMode.Dark)

    @Test
    fun delayedSpinnerInLightThemeMatchesReference() = captureLoading(ThemeMode.Light)

    private fun captureLoading(themeMode: ThemeMode) = runDesktopComposeUiTest {
        mainClock.autoAdvance = false
        setGoldenContent(this, themeMode = themeMode) {
            ProjectsContent(
                TestProjectsComponent(
                    initialState = UiState.Loading,
                    projectPath = listOf(ProjectBreadcrumb(1, "Backend")),
                ),
            )
        }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(1_200)
        onNodeWithTag(LOADING_INDICATOR_TAG).assertIsDisplayed()
        captureGolden(this, PROJECTS_SCREEN_TAG)
    }
}
