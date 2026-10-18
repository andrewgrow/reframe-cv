package reframecv.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Keeps layout gaps consistent at the selected UI scale. */
@Immutable
data class UiSpacing(val small: Dp, val medium: Dp, val large: Dp)

/** Keeps component sizes consistent at the selected UI scale. */
@Immutable
data class UiDimensions(
    val navigationWidth: Dp,
    val providerModelMenuMaxHeight: Dp,
    val providerTableMinWidth: Dp,
    val providerStatusWidth: Dp,
    val projectTreeTextMinWidth: Dp,
    val dashboardSectionMinWidth: Dp,
    val vacancyTableMinWidth: Dp,
    val resumeTableMinWidth: Dp,
    val resumeLanguageWidth: Dp,
    val resumeHeaderMinWidth: Dp,
    val resumeNameKeywordsMinWidth: Dp,
    val resumeSectionControlsMinWidth: Dp,
    val documentDateWidth: Dp,
    val vacancyDescriptionMinHeight: Dp,
    val dialogMinWidth: Dp,
    val dialogDefaultWidth: Dp,
    val buttonHeight: Dp,
    val projectActionWidth: Dp,
    val projectActionHeight: Dp,
    val projectHelpMaxWidth: Dp,
    val iconSize: Dp,
    val iconButtonSize: Dp,
    val loadingIndicatorSize: Dp,
    val scrollControlSize: Dp,
    val scrollbarThickness: Dp,
    val scrollThumbMinHeight: Dp,
    val resumePhotoWidth: Dp,
    val resumePhotoDialogWidth: Dp,
)

/** Lets the whole interface change scale without separate adjustments in each screen. */
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
        dimensions = createUiDimensions(factor),
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

private fun createUiDimensions(factor: Float): UiDimensions = UiDimensions(
    navigationWidth = 220.dp * factor,
    providerModelMenuMaxHeight = 280.dp * factor,
    providerTableMinWidth = 480.dp * factor,
    providerStatusWidth = 160.dp * factor,
    projectTreeTextMinWidth = 120.dp * factor,
    dashboardSectionMinWidth = 160.dp * factor,
    vacancyTableMinWidth = 480.dp * factor,
    resumeTableMinWidth = 320.dp * factor,
    resumeLanguageWidth = 144.dp * factor,
    resumeHeaderMinWidth = 440.dp * factor,
    resumeNameKeywordsMinWidth = 480.dp * factor,
    resumeSectionControlsMinWidth = 520.dp * factor,
    documentDateWidth = 104.dp * factor,
    vacancyDescriptionMinHeight = 80.dp * factor,
    dialogMinWidth = 280.dp * factor,
    dialogDefaultWidth = 560.dp * factor,
    buttonHeight = 48.dp * factor,
    projectActionWidth = 160.dp * factor,
    projectActionHeight = 40.dp * factor,
    projectHelpMaxWidth = 560.dp * factor,
    iconSize = 24.dp * factor,
    iconButtonSize = 48.dp * factor,
    loadingIndicatorSize = 40.dp * factor,
    scrollControlSize = 32.dp * factor,
    scrollbarThickness = 6.dp * factor,
    scrollThumbMinHeight = 32.dp * factor,
    resumePhotoWidth = 160.dp * factor,
    resumePhotoDialogWidth = 140.dp * factor,
)
