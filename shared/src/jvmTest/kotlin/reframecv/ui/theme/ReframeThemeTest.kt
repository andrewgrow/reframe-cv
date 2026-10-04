package reframecv.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.ui.compose.common.FillButton

@OptIn(ExperimentalTestApi::class)
class ReframeThemeTest {
    @Test
    fun changingScaleUpdatesRenderedButtonAndText() = runComposeUiTest {
        var scale by mutableStateOf(UiScale.Default)
        setContent {
            ReframeTheme(uiScale = scale) { FillButton("Scale", onClick = {}) }
        }
        val node = onNodeWithText("Scale")
        val baselineHeight = node.fetchSemanticsNode().boundsInRoot.height
        fun fontSize(): Float {
            val results = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            return results.single().layoutInput.style.fontSize.value
        }
        val baselineFontSize = fontSize()
        runOnIdle { scale = UiScale(75) }
        assertEquals(baselineHeight * scale.factor, node.fetchSemanticsNode().boundsInRoot.height)
        assertEquals(baselineFontSize * scale.factor, fontSize())
        runOnIdle { scale = UiScale(150) }
        assertEquals(baselineHeight * scale.factor, node.fetchSemanticsNode().boundsInRoot.height)
        assertEquals(baselineFontSize * scale.factor, fontSize())
    }
}
