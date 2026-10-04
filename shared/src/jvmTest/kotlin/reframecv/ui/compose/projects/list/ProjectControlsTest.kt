package reframecv.ui.compose.projects.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class ProjectControlsTest {
    @Test
    fun manyActionsUseTwoRowsStartAtFirstColumnAndRemainAccessible() = runComposeUiTest {
        var clickedIndex: Int? = null
        val actions = List(12) { index ->
            ProjectAction("Action $index", onClick = { clickedIndex = index })
        }
        setContent {
            ReframeTheme {
                Box(Modifier.width(300.dp)) { ProjectControls(actions) }
            }
        }
        val first = onNodeWithText("Action 0").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val second = onNodeWithText(
            "Action 1",
        ).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(first.left, second.left)
        assertTrue(second.top >= first.bottom)
        onNodeWithText("Action 0").performClick()
        assertEquals(0, clickedIndex)

        onNodeWithTag(PROJECT_CONTROLS_TAG).performScrollToIndex(actions.lastIndex)
        onNodeWithText("Action 11").assertIsDisplayed().performClick()
        assertEquals(11, clickedIndex)
    }
}
