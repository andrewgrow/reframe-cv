package reframecv.ui.compose.settings.runners

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.settings.runners.RunnerResultAction

@OptIn(ExperimentalTestApi::class)
class RunnerHistoryActionsScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun priorImportsHaveNoActionsAndPriorDismissalsOfferRestore() = runDesktopComposeUiTest(
        screenSize = ScreenSize(1000.dp, 1000.dp),
    ) {
        val component = PreviewRunnerRunsComponent(runnerTableFixture())
        setAndCaptureGolden(this, screenSize = ScreenSize(1000.dp, 1000.dp)) {
            RunnerRunsContent(component)
        }
        onNodeWithTag("runner.result.convert.2").assertDoesNotExist()
        onNodeWithTag("runner.result.review.2").assertDoesNotExist()
        onNodeWithTag("runner.result.convert.3").assertDoesNotExist()
        onNodeWithTag("runner.result.review.3").performScrollTo().performClick()
        assertEquals(3L to RunnerResultAction.Restore, component.action)
    }

    @Test
    fun explicitRestoreOffersConvertAndDismissDespiteAnEarlierDismissal() =
        runDesktopComposeUiTest {
            val initial = runnerTableFixture()
            val view = initial.view!!
            val page = view.page!!
            val component = PreviewRunnerRunsComponent(
                initial.copy(
                    view = view.copy(
                        page = page.copy(
                            savedResults = page.savedResults.map {
                                if (it.id ==
                                    3L
                                ) {
                                    it.copy(status = RunnerResultStatus.Restored)
                                } else {
                                    it
                                }
                            },
                        ),
                    ),
                ),
            )
            setGoldenContent(this) { RunnerRunsContent(component) }
            onNodeWithTag("runner.result.convert.3").performScrollTo().performClick()
            assertEquals(3L to RunnerResultAction.Convert, component.action)
            onNodeWithTag("runner.result.review.3").performClick()
            assertEquals(3L to RunnerResultAction.Dismiss, component.action)
        }
}
