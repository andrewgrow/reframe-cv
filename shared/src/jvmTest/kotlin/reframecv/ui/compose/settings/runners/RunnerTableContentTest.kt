package reframecv.ui.compose.settings.runners

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.settings.runners.RunnerResultAction
import reframecv.ui.threading.runOnUiThread

@OptIn(ExperimentalTestApi::class)
class RunnerTableContentTest : GoldenScreenshotTest() {
    @Test
    fun reviewButtonsAndLocalPaginationAreSeparateFromLoading() = runDesktopComposeUiTest {
        val initial = runnerTableFixture()
        val view = initial.view!!
        val component =
            PreviewRunnerRunsComponent(
                initial.copy(
                    view = view.copy(
                        page = view.page!!.copy(
                            savedCount = 30,
                            visibleCount = 30,
                            nextOffset = 25,
                        ),
                    ),
                ),
            )
        setGoldenContent(this) { RunnerRunsContent(component) }
        onNodeWithTag("runner.result.convert.1").performClick()
        assertEquals(1L to RunnerResultAction.Convert, component.action)
        onNodeWithTag("runner.result.review.1").performClick()
        assertEquals(1L to RunnerResultAction.Dismiss, component.action)
        onNodeWithTag("runner.results.next").performClick()
        assertEquals(25, component.offset)
        assertEquals(0, component.loads)
        onNodeWithTag("runner.results.more").performClick()
        assertEquals(1, component.loads)
    }

    @Test
    fun fullCellValueCanBeReadAndActionsStayFixedAfterHorizontalScroll() = runDesktopComposeUiTest {
        setGoldenContent(this) {
            RunnerRunsContent(PreviewRunnerRunsComponent(runnerTableFixture()))
        }
        val description = runnerCells(runnerTableFixture().view!!.page!!.savedResults.first())
            .getValue("descriptionText")
        val cell = onAllNodesWithTag("runner.cell.descriptionText")[0]
        cell.performScrollTo()
        val layouts = mutableListOf<TextLayoutResult>()
        cell.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(3, layouts.single().lineCount)
        assertTrue(
            layouts.single().isLineEllipsized(2) ||
                layouts.single().layoutInput.text.text.endsWith("…"),
        )
        cell.performClick()
        onNodeWithTag("runner.result.cell.dialog").assertIsDisplayed()
        onNode(
            hasText(description) and hasAnyAncestor(hasTestTag("runner.result.cell.dialog")),
        ).assertIsDisplayed()
        onNode(
            hasText("Close") and hasAnyAncestor(hasTestTag("runner.result.cell.dialog")),
        ).performClick()
        onNodeWithTag("scroll.to.right").performClick()
        onNodeWithTag("runner.result.convert.1").assertIsDisplayed()
        onNodeWithTag("scroll.to.left").performClick()
        onAllNodesWithTag("runner.cell.id")[0].assertIsDisplayed()
    }

    @Test
    fun horizontalThumbCanBeDraggedWhileActionButtonsRemainVisible() = runDesktopComposeUiTest {
        setGoldenContent(this) {
            RunnerRunsContent(PreviewRunnerRunsComponent(runnerTableFixture()))
        }
        val track = onNodeWithTag("scroll.horizontal.track")
        track.performTouchInput {
            swipe(Offset(5f, height / 2f), Offset(width * 0.8f, height / 2f), 300)
        }
        val progress = track.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        assertTrue(progress.current > 0)
        onNodeWithTag("runner.result.convert.1").assertIsDisplayed()
    }

    @Test
    fun importDialogButtonsDoNotOverlap() = runDesktopComposeUiTest {
        setGoldenContent(this) {
            RunnerRunsContent(
                PreviewRunnerRunsComponent(runnerTableFixture().copy(importResultId = 1)),
            )
        }
        val cancel = onNodeWithTag("runner.import.cancel").fetchSemanticsNode().boundsInRoot
        val create = onNodeWithTag("runner.import.confirm").fetchSemanticsNode().boundsInRoot
        assertTrue(cancel.right <= create.left)
    }

    @Test
    fun columnsFitShortIdsAndLimitLargeDescriptionsWithMatchingHeaders() = runDesktopComposeUiTest {
        setGoldenContent(this) {
            RunnerRunsContent(PreviewRunnerRunsComponent(runnerTableFixture()))
        }
        onNodeWithTag("runner.header.id").assertWidthIsEqualTo(100.dp)
        onAllNodesWithTag("runner.cell.id")[0].assertWidthIsEqualTo(100.dp)
        onNodeWithTag("runner.header.descriptionText").assertWidthIsEqualTo(320.dp)
        onAllNodesWithTag("runner.cell.descriptionText")[0].assertWidthIsEqualTo(320.dp)
    }

    @Test
    fun dashboardProjectIsPreselectedAndCanBeChangedBeforeImport() = runDesktopComposeUiTest {
        val state = runnerTableFixture()
        val other = state.projects.single().copy(id = 2, name = "Another project")
        val component = PreviewRunnerRunsComponent(
            state.copy(
                importResultId = 1,
                preferredProjectId = 1,
                projects = state.projects + other,
            ),
        )
        setGoldenContent(this) { RunnerRunsContent(component) }
        onNodeWithTag("runner.import.confirm").assertIsEnabled()
        onNodeWithTag("runner.import.project.2").performClick()
        onNodeWithTag("runner.import.confirm").performClick()
        assertEquals(2L, component.project)
    }

    @Test
    fun filterIconsToggleIndependentlyAndCannotChangeWhileProcessing() = runDesktopComposeUiTest {
        val component = PreviewRunnerRunsComponent(runnerTableFixture())
        setGoldenContent(this) { RunnerRunsContent(component) }
        val dismissed = onNodeWithTag("runner.filter.dismissed")
        val imported = onNodeWithTag("runner.filter.imported")
        dismissed.assertIsOff().performClick().assertIsOn()
        assertEquals(false, component.state.value.filter.showDismissed)
        assertEquals(true, component.state.value.filter.showImported)
        imported.performClick().assertIsOn()
        assertEquals(false, component.state.value.filter.showImported)
        dismissed.performClick().assertIsOff()
        assertEquals(true, component.state.value.filter.showDismissed)
        runOnUiThread { component.state.value = component.state.value.copy(working = true) }
        dismissed.assertIsNotEnabled()
        imported.assertIsNotEnabled()
    }
}
