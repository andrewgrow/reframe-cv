package reframecv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UiTokensTest {
    @Test
    fun defaultScalePreservesBaseline() {
        val tokens = createUiTokens(UiScale.Default)
        assertEquals(16.dp, tokens.spacing.medium)
        assertEquals(48.dp, tokens.dimensions.buttonHeight)
        assertEquals(220.dp, tokens.dimensions.navigationWidth)
        assertEquals(Typography(), tokens.typography)
    }

    @Test
    fun sizesAndTypographyScaleTogetherWhileOutlineAndTimingStayConstant() {
        val baseline = createUiTokens(UiScale.Default)
        for (percent in listOf(75, 150)) {
            val scale = UiScale(percent)
            val tokens = createUiTokens(scale)
            assertEquals(baseline.spacing.medium * scale.factor, tokens.spacing.medium)
            assertEquals(
                baseline.dimensions.buttonHeight * scale.factor,
                tokens.dimensions.buttonHeight,
            )
            assertEquals(
                baseline.typography.bodyLarge.fontSize * scale.factor,
                tokens.typography.bodyLarge.fontSize,
            )
            assertEquals(
                baseline.typography.bodyLarge.lineHeight * scale.factor,
                tokens.typography.bodyLarge.lineHeight,
            )
            assertEquals(baseline.outlineWidth, tokens.outlineWidth)
            assertEquals(baseline.fillAnimationMillis, tokens.fillAnimationMillis)
        }
    }

    @Test
    fun scaleStepsRespectLimitsAndRejectInvalidValues() {
        assertEquals(UiScale(95), UiScale.Default.smaller())
        assertEquals(UiScale(105), UiScale.Default.larger())
        assertEquals(UiScale(75), UiScale(75).smaller())
        assertEquals(UiScale(150), UiScale(150).larger())
        assertFailsWith<IllegalArgumentException> { UiScale(0) }
        assertFailsWith<IllegalArgumentException> { UiScale(151) }
    }
}
