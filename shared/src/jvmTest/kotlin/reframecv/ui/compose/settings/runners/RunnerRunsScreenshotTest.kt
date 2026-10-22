package reframecv.ui.compose.settings.runners

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.value.MutableValue
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunState
import reframecv.runners.RemoteRunnerRun
import reframecv.runners.RunnerResultsPage
import reframecv.runners.VacancySearchResult
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.settings.runners.RunnerDraft
import reframecv.ui.components.settings.runners.RunnerRunsComponent
import reframecv.ui.components.settings.runners.RunnerRunsState
import reframecv.usecase.RunnerRunView

@OptIn(ExperimentalTestApi::class)
class RunnerRunsScreenshotTest : GoldenScreenshotTest() {
    private val runner = RunnerDraft(
        name = "Example Kotlin search",
        searchUrls = "https://www.linkedin.com/jobs/search/?keywords=kotlin",
    ).applyTo(RunnerDraft.newRunner(1)).copy(id = 1)
    private val run = ConnectionRun(
        id = 1,
        runnerId = 1,
        configurationSnapshot = runner.configuration,
        state = ConnectionRunState.Succeeded,
        externalId = "exampleRun1",
        startedAt = 1792684800000,
        finishedAt = 1792684820000,
    )
    private val result = VacancySearchResult(
        "101", "Senior mobile developer", "ExampleCo", "Build Kotlin applications.\nWork with QA.",
        "https://www.linkedin.com/jobs/view/101", "Vienna, Austria", "2026-10-22",
        employmentType = "Full-time", seniorityLevel = "Senior",
    )
    private fun state() = RunnerRunsState(runner, runs = listOf(run), loadingHistory = false)
    private fun results() = state().copy(
        view = RunnerRunView(
            run,
            RemoteRunnerRun(
                "exampleRun1",
                ConnectionRunState.Succeeded,
                "dataset1",
                costUsd = 0.01,
            ),
            RunnerResultsPage(listOf(result), 0, 50, 100, 1),
        ),
    )

    @Test
    fun persistedHistoryMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this) { RunnerRunsContent(PreviewRunnerRunsComponent(state())) }
    }

    @Test
    fun resultsAndExpandableDetailsMatchReferenceAndNextUsesOffset() = runDesktopComposeUiTest {
        val component = PreviewRunnerRunsComponent(results())
        setGoldenContent(this) { RunnerRunsContent(component) }
        onNodeWithTag("runner.result.details.101").performClick()
        onNodeWithText(result.description).performScrollTo().assertIsDisplayed()
        onNodeWithTag("runner.results.next").performScrollTo().performClick()
        assertEquals(50, component.offset)
        captureGolden(this)
    }

    @Test
    fun expiredKeyErrorAtNarrowWidthMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, screenSize = ScreenSize(360.dp, 800.dp)) {
            RunnerRunsContent(
                PreviewRunnerRunsComponent(state().copy(error = "Apify API token expired.")),
            )
        }
    }
}

private class PreviewRunnerRunsComponent(initial: RunnerRunsState) : RunnerRunsComponent {
    override val state = MutableValue(initial)
    var offset: Int? = null
    override fun onStart() = Unit
    override fun onSelect(id: Long) = Unit
    override fun onRefresh() = Unit
    override fun onPage(offset: Int) {
        this.offset = offset
    }
    override fun onRetryHistory() = Unit
    override fun onBack() = Unit
}
