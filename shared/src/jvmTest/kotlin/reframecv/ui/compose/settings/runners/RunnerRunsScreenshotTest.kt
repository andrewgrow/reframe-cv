package reframecv.ui.compose.settings.runners

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.domain.models.providers.RunnerResultFilter
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class RunnerRunsScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun dynamicDatasetTableMatchesReference() = runDesktopComposeUiTest(
        screenSize = ScreenSize(1100.dp, 850.dp),
    ) {
        setAndCaptureGolden(this, screenSize = ScreenSize(1100.dp, 850.dp)) {
            RunnerRunsContent(PreviewRunnerRunsComponent(runnerTableFixture()))
        }
    }

    @Test
    fun horizontalScrollKeepsActionsVisibleInLightTheme() = runDesktopComposeUiTest {
        setGoldenContent(this, themeMode = ReframeTheme.ThemeMode.Light) {
            RunnerRunsContent(PreviewRunnerRunsComponent(runnerTableFixture()))
        }
        onNodeWithTag("scroll.to.right").performClick()
        onNodeWithTag("runner.result.convert.1").assertIsDisplayed()
        captureGolden(this)
    }

    @Test
    fun narrowDatasetTableMatchesReference() = runDesktopComposeUiTest(
        screenSize = ScreenSize(360.dp, 800.dp),
    ) {
        setAndCaptureGolden(this, screenSize = ScreenSize(360.dp, 800.dp)) {
            RunnerRunsContent(PreviewRunnerRunsComponent(runnerTableFixture()))
        }
    }

    @Test
    fun emptyDatasetMatchesReference() = runDesktopComposeUiTest {
        val state = runnerTableFixture()
        val view = state.view!!
        setAndCaptureGolden(this) {
            RunnerRunsContent(
                PreviewRunnerRunsComponent(
                    state.copy(
                        view = view.copy(
                            run = view.run.copy(
                                resultsComplete = true,
                                resultTotal = 0,
                                nextResultOffset = 0,
                            ),
                            page = view.page!!.copy(
                                savedResults = emptyList(),
                                savedCount = 0,
                                visibleCount = 0,
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun importProjectSelectionMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, captureTag = "runner.import.dialog") {
            RunnerRunsContent(
                PreviewRunnerRunsComponent(runnerTableFixture().copy(importResultId = 1)),
            )
        }
    }

    @Test
    fun persistedHistoryMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this) {
            RunnerRunsContent(PreviewRunnerRunsComponent(runnerTableFixture().copy(view = null)))
        }
    }

    @Test
    fun expiredKeyErrorAtNarrowWidthMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, screenSize = ScreenSize(360.dp, 800.dp)) {
            RunnerRunsContent(
                PreviewRunnerRunsComponent(
                    runnerTableFixture().copy(
                        view = null,
                        error = "Apify API token expired.",
                    ),
                ),
            )
        }
    }

    @Test
    fun hiddenResultsKeepFiltersAndLoadMoreAvailableInNarrowLayout() = runDesktopComposeUiTest(
        screenSize = ScreenSize(360.dp, 800.dp),
    ) {
        val initial = runnerTableFixture()
        setAndCaptureGolden(this, screenSize = ScreenSize(360.dp, 800.dp)) {
            RunnerRunsContent(
                PreviewRunnerRunsComponent(
                    initial.copy(
                        filter = RunnerResultFilter(showDismissed = false, showImported = false),
                        view = initial.view!!.copy(
                            page = initial.view!!.page!!.copy(
                                savedResults = emptyList(),
                                visibleCount = 0,
                                nextOffset = null,
                            ),
                        ),
                    ),
                ),
            )
        }
        onNodeWithTag("runner.filter.dismissed").assertIsDisplayed()
        onNodeWithTag("runner.filter.imported").assertIsDisplayed()
        onNodeWithTag("runner.results.more").assertIsDisplayed()
    }
}
