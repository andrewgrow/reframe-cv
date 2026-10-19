package reframecv.ui.compose.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.ui.components.common.section.CollapsibleSectionState
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class CollapsibleSectionTest {
    @Test
    fun restoresExpandedWithoutAnimationButAnimatesCollapseClick() = checkRestoration(true)

    @Test
    fun restoresCollapsedWithoutAnimationButAnimatesExpandClick() = checkRestoration(false)

    private fun checkRestoration(expanded: Boolean) = runDesktopComposeUiTest {
        var state by mutableStateOf(CollapsibleSectionState(loading = true))
        setContent {
            ReframeTheme {
                CollapsibleSection("Job details", state, {
                    state = state.copy(expanded = !state.expanded)
                }, "section") {
                    Box(Modifier.height(200.dp).testTag("section.body"))
                }
            }
        }
        mainClock.autoAdvance = false
        val section = onNodeWithTag("section")
        val collapsedHeight = section.fetchSemanticsNode().size.height
        runOnIdle { state = CollapsibleSectionState(expanded = expanded) }
        mainClock.advanceTimeByFrame()
        val restoredHeight = section.fetchSemanticsNode().size.height
        if (expanded) {
            assertTrue(restoredHeight >= collapsedHeight + 200)
        } else {
            assertEquals(collapsedHeight, restoredHeight)
            onNodeWithTag("section.body").assertDoesNotExist()
        }
        mainClock.advanceTimeBy(400)
        assertEquals(restoredHeight, section.fetchSemanticsNode().size.height)
        onNodeWithTag("section.toggle").performClick()
        mainClock.advanceTimeBy(100)
        val animatedHeight = section.fetchSemanticsNode().size.height
        if (expanded) {
            assertTrue(animatedHeight in (collapsedHeight + 1) until restoredHeight)
        } else {
            assertTrue(animatedHeight > collapsedHeight)
            assertTrue(animatedHeight < collapsedHeight + 200)
        }
        mainClock.advanceTimeBy(400)
        if (expanded) {
            assertEquals(collapsedHeight, section.fetchSemanticsNode().size.height)
        } else {
            assertTrue(section.fetchSemanticsNode().size.height >= collapsedHeight + 200)
        }
        mainClock.autoAdvance = true
    }
}
