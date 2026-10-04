package reframecv.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class UiSpacing(val small: Dp, val medium: Dp, val large: Dp)

@Immutable
data class UiDimensions(
    val navigationWidth: Dp,
    val buttonHeight: Dp,
    val projectActionWidth: Dp,
    val projectHelpMaxWidth: Dp,
    val iconSize: Dp,
    val iconButtonSize: Dp,
)

@Immutable
data class UiTokens(
    val scale: UiScale,
    val spacing: UiSpacing,
    val dimensions: UiDimensions,
    val typography: Typography,
    val buttonContentPadding: PaddingValues,
    val textButtonContentPadding: PaddingValues,
    val outlineWidth: Dp = 1.dp,
    val fillAnimationMillis: Int = 300,
    val disabledContentAlpha: Float = 0.38f,
)

/** Baseline values match the existing UI and Material typography at 100%. */
internal fun createUiTokens(scale: UiScale): UiTokens {
    val factor = scale.factor
    val typography = Typography()
    return UiTokens(
        scale = scale,
        spacing = UiSpacing(8.dp * factor, 16.dp * factor, 24.dp * factor),
        dimensions = UiDimensions(
            navigationWidth = 220.dp * factor,
            buttonHeight = 48.dp * factor,
            projectActionWidth = 200.dp * factor,
            projectHelpMaxWidth = 560.dp * factor,
            iconSize = 24.dp * factor,
            iconButtonSize = 48.dp * factor,
        ),
        typography = typography.copy(
            displayLarge = typography.displayLarge.scaled(factor),
            displayMedium = typography.displayMedium.scaled(factor),
            displaySmall = typography.displaySmall.scaled(factor),
            headlineLarge = typography.headlineLarge.scaled(factor),
            headlineMedium = typography.headlineMedium.scaled(factor),
            headlineSmall = typography.headlineSmall.scaled(factor),
            titleLarge = typography.titleLarge.scaled(factor),
            titleMedium = typography.titleMedium.scaled(factor),
            titleSmall = typography.titleSmall.scaled(factor),
            bodyLarge = typography.bodyLarge.scaled(factor),
            bodyMedium = typography.bodyMedium.scaled(factor),
            bodySmall = typography.bodySmall.scaled(factor),
            labelLarge = typography.labelLarge.scaled(factor),
            labelMedium = typography.labelMedium.scaled(factor),
            labelSmall = typography.labelSmall.scaled(factor),
        ),
        buttonContentPadding = PaddingValues(horizontal = 24.dp * factor, vertical = 8.dp * factor),
        textButtonContentPadding = PaddingValues(
            horizontal = 12.dp * factor,
            vertical =
                8.dp * factor,
        ),
    )
}

private fun TextStyle.scaled(factor: Float): TextStyle = copy(
    fontSize = fontSize * factor,
    lineHeight = lineHeight * factor,
    letterSpacing = letterSpacing * factor,
)
