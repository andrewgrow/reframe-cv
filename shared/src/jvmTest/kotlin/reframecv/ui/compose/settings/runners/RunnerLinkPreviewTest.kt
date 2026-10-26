package reframecv.ui.compose.settings.runners

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.testing.GoldenScreenshotTest

@OptIn(ExperimentalTestApi::class)
class RunnerLinkPreviewTest : GoldenScreenshotTest() {
    @Test
    fun opensRecognizedCellUrlThroughTheBrowserHandler() = runDesktopComposeUiTest {
        val opened = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        setGoldenContent(this) {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                RunnerRunsContent(PreviewRunnerRunsComponent(runnerTableFixture()))
            }
        }
        onAllNodesWithTag("runner.cell.companyWebsite")[0].performScrollTo().performClick()
        onNodeWithTag("runner.result.cell.dialog").assertIsDisplayed()
        val value = "https://company1.example.test"
        val text = onNode(
            hasText(value) and hasAnyAncestor(hasTestTag("runner.result.cell.dialog")),
        ).assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        text.performMouseInput { click(layouts.single().getBoundingBox(5).center) }
        waitForIdle()
        assertEquals(listOf(value), opened)
        captureGolden(this, "runner.result.cell.dialog")
    }
}
