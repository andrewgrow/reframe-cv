package reframecv.ui.compose.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.testing.GoldenScreenshotTest

@OptIn(ExperimentalTestApi::class)
class FastScrollColumnTest : GoldenScreenshotTest() {
    @Test
    fun buttonsReachBothEndsAndDraggingThumbWorksInBothDirections() = runDesktopComposeUiTest {
        val state = ScrollState(0)
        setGoldenContent(this) {
            FastScrollColumn(scrollState = state) {
                repeat(100) { Text("Line $it", Modifier.height(32.dp)) }
            }
        }
        onNodeWithTag("scroll.to.top").assertIsNotEnabled()
        onNodeWithTag("scroll.to.bottom").performClick()
        onNodeWithText("Line 99").assertIsDisplayed()
        assertEquals(state.maxValue, state.value)
        onNodeWithTag("scroll.to.bottom").assertIsNotEnabled()
        onNodeWithTag("scroll.to.top").performClick()
        waitForIdle()
        assertEquals(0, state.value)
        onNodeWithTag("scroll.track").performTouchInput {
            swipe(Offset(center.x, 16f), Offset(center.x, height - 16f))
        }
        waitForIdle()
        assertTrue(state.value > state.maxValue / 2)
        onNodeWithTag("scroll.track").performTouchInput {
            swipe(Offset(center.x, height - 16f), Offset(center.x, 16f))
        }
        waitForIdle()
        assertTrue(state.value < state.maxValue / 2)
    }

    @Test
    fun trackClickAndMouseWheelCanScrollWithoutDragging() = runDesktopComposeUiTest {
        val state = ScrollState(0)
        setGoldenContent(this) {
            FastScrollColumn(scrollState = state) {
                repeat(100) { Text("Line $it", Modifier.height(32.dp)) }
            }
        }
        onNodeWithTag("scroll.track").performTouchInput {
            click(Offset(center.x, height * 0.75f))
        }
        waitForIdle()
        assertTrue(state.value > state.maxValue / 2)
        onNodeWithTag("scroll.to.top").performClick()
        waitForIdle()
        onNodeWithTag("scroll.track").performMouseInput {
            enter(center)
            scroll(300f)
        }
        waitForIdle()
        assertTrue(state.value > 0)
    }

    @Test
    fun shortContentDisablesEndButtonsAndTrackClickDoesNotMoveIt() = runDesktopComposeUiTest {
        val state = ScrollState(0)
        setGoldenContent(this) {
            FastScrollColumn(scrollState = state) { Text("Short content") }
        }
        onNodeWithTag("scroll.to.top").assertIsNotEnabled()
        onNodeWithTag("scroll.to.bottom").assertIsNotEnabled()
        onNodeWithTag("scroll.track").performTouchInput { click(center) }
        assertEquals(0, state.value)
    }
}
