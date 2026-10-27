package reframecv.ui.compose.settings.runners

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import reframecv.domain.models.providers.ConnectionOperation
import reframecv.domain.models.providers.RunnerResult
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.settings.runners.RunnerDraft
import reframecv.ui.components.settings.runners.RunnerEditorState
import reframecv.ui.components.settings.runners.TestRunnerEditorComponent

@OptIn(ExperimentalTestApi::class)
class AmsRunnerScreenshotTest : GoldenScreenshotTest() {
    private val draft = RunnerDraft(
        name = "Android Vienna AMS",
        query = "Android",
        operation = ConnectionOperation.AmsJobSearch,
        maxResults = "50",
    )

    @Test
    fun newRunnerCanSelectAmsAndEditSearchOptions() = runDesktopComposeUiTest {
        val component = TestRunnerEditorComponent(RunnerEditorState(RunnerDraft.newRunner(1)))
        setGoldenContent(this) { RunnerEditorContent(component) }
        onNodeWithTag("runner.operation").performClick()
        onNodeWithText("AMS Austria Job Scraper").performClick()
        assertEquals(ConnectionOperation.AmsJobSearch, component.state.value.draft.operation)
        onNodeWithTag("runner.query").assertExists()
        onNodeWithTag("runner.radius").performScrollTo().performClick()
        onNodeWithText("20").performClick()
        assertEquals("20", component.state.value.draft.radiusKm)
        onNodeWithTag("runner.limit").performScrollTo()
        onNodeWithTag("runner.external").performClick()
        assertEquals(false, component.state.value.draft.includeExternalJobs)
    }

    @Test
    fun amsEditorShowsSearchSettings() = runDesktopComposeUiTest(
        screenSize = ScreenSize(700.dp, 1050.dp),
    ) {
        setAndCaptureGolden(this, screenSize = ScreenSize(700.dp, 1050.dp)) {
            RunnerEditorContent(
                TestRunnerEditorComponent(
                    RunnerEditorState(draft.applyTo(RunnerDraft.newRunner(1))),
                ),
            )
        }
    }

    @Test
    fun amsDatasetUsesDynamicColumnsAndManualReviewActions() = runDesktopComposeUiTest(
        screenSize = ScreenSize(1100.dp, 850.dp),
    ) {
        val initial = runnerTableFixture()
        val view = initial.view!!
        val rows = (1..3).map { index ->
            val json = buildJsonObject {
                put("referenceId", "80$index")
                put("title", "Android Engineer")
                put("employer", "Example Mobile")
                put("description", "Build Kotlin applications. Work with testing and design teams.")
                put("externalSource", if (index == 1) "devjobs.at" else "karriere.at")
                put("externalUrl", "https://jobs.example.test/$index")
                put("publishedDate", "2026-10-27T00:00:00Z")
                put("salary", "65000 EUR/yearly")
            }
            RunnerResult(
                index.toLong(),
                1,
                index - 1,
                "AMS",
                "80$index",
                json.toString(),
                123,
                status = if (index == 3) RunnerResultStatus.Dismissed else RunnerResultStatus.New,
            )
        }
        val columns = listOf(
            "referenceId",
            "title",
            "employer",
            "description",
            "externalSource",
            "externalUrl",
            "publishedDate",
            "salary",
        )
        setAndCaptureGolden(this, screenSize = ScreenSize(1100.dp, 850.dp)) {
            RunnerRunsContent(
                PreviewRunnerRunsComponent(
                    initial.copy(
                        runner = draft.applyTo(initial.runner),
                        view = view.copy(
                            run = view.run.copy(resultColumns = columns),
                            page = view.page!!.copy(
                                savedResults = rows,
                                savedCount = 3,
                                visibleCount = 3,
                            ),
                        ),
                    ),
                ),
            )
        }
    }
}
