package reframecv.ui.compose.common

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class FillButtonTest {
    @Test
    fun textColorChangesGraduallyOnHoverEnterAndExit() = runComposeUiTest {
        val source = MutableInteractionSource()
        val hover = HoverInteraction.Enter()
        setContent {
            ReframeTheme {
                FillButton(
                    text = "Animated text",
                    onClick = {},
                    contentColor = Color.Black,
                    filledContentColor = Color.White,
                    interactionSource = source,
                )
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false

        fun textColor(): Color {
            val results = mutableListOf<TextLayoutResult>()
            onNodeWithText("Animated text").performSemanticsAction(
                SemanticsActions.GetTextLayoutResult,
            ) { it(results) }
            return results.single().layoutInput.style.color
        }

        assertEquals(Color.Black, textColor())
        runOnIdle { assertTrue(source.tryEmit(hover)) }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(100)
        val enteringColor = textColor()
        assertTrue(enteringColor.red > 0f && enteringColor.red < 1f)
        mainClock.advanceTimeBy(200)
        assertEquals(Color.White, textColor())

        runOnIdle { assertTrue(source.tryEmit(HoverInteraction.Exit(hover))) }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(100)
        val exitingColor = textColor()
        assertTrue(exitingColor.red > 0f && exitingColor.red < 1f)
        mainClock.advanceTimeBy(200)
        assertEquals(Color.Black, textColor())
    }
}
