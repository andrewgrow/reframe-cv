package reframecv.ui.compose.settings.runners

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.testing.GoldenScreenshotTest

@OptIn(ExperimentalTestApi::class)
class RunnerRelevanceScreenshotTest : GoldenScreenshotTest() {
    private fun scored(): PreviewRunnerRunsComponent {
        val state = runnerTableFixture()
        val view = state.view!!
        val page = view.page!!
        return PreviewRunnerRunsComponent(
            state.copy(
                view = view.copy(
                    page = page.copy(
                        savedResults = page.savedResults.mapIndexed { index, row ->
                            when (index) {
                                0 -> row.copy(
                                    relevanceScore = 92,
                                    relevancePrompt = "Find Android app development roles.",
                                )

                                1 -> row.copy(
                                    relevanceScore = 0,
                                    relevancePrompt = "Find Android app development roles.",
                                )

                                else -> row
                            }
                        },
                    ),
                ),
            ),
        )
    }

    @Test
    fun scoresIncludeZeroAndOpenTheExactSavedPrompt() = runDesktopComposeUiTest {
        val component = scored()
        setGoldenContent(this) { RunnerRunsContent(component) }
        onNodeWithTag("runner.relevance.score.1").assertTextContains("92%")
        onNodeWithTag("runner.relevance.score.2").assertTextContains("0%")
        captureGolden(this)
        onNodeWithTag("runner.relevance.score.1").performClick()
        onNodeWithTag("runner.relevance.result").assertExists()
    }

    @Test
    fun criteriaDialogShowsThePromptAndEscapeReturnsToResults() = runDesktopComposeUiTest {
        val component = scored()
        component.relevance.view(1)
        setGoldenContent(this) { RunnerRunsContent(component) }
        captureGolden(this, "runner.relevance.result")
        onNodeWithTag("runner.relevance.result").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("runner.relevance.result").assertDoesNotExist()
        onNodeWithTag("runner.runs.page").assertExists()
    }

    @Test
    fun aiIconOpensSharedRequestDialogWithoutAResumeOrProject() = runDesktopComposeUiTest {
        val component = PreviewRunnerRunsComponent(runnerTableFixture())
        setGoldenContent(this) { RunnerRunsContent(component) }
        onNodeWithTag("runner.relevance.assess.1").performClick()
        assertEquals(1L, component.relevance.requested)
        captureGolden(this, "ai.request.dialog")
        onNodeWithTag("ai.request.dialog").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("ai.request.dialog").assertDoesNotExist()
        onNodeWithTag("runner.runs.page").assertExists()
    }
}
