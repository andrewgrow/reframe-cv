package reframecv.ui.compose.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import reframecv.ui.theme.ReframeTheme

/** Reports the active editor's footer height to the surrounding navigation shell. */
internal val LocalDocumentFooterHeight = staticCompositionLocalOf<(Dp?) -> Unit> { {} }

/** Reserves one notification line above a standard action row at the current UI scale. */
@Composable
internal fun documentFooterMinHeight(): Dp {
    val tokens = ReframeTheme.tokens
    val line = with(LocalDensity.current) { tokens.typography.bodyMedium.lineHeight.toDp() }
    return tokens.dimensions.buttonHeight + tokens.spacing.medium * 2 + tokens.spacing.small + line
}

/** Keeps editor footers consistent and aligns the navigation footer when content needs more room. */
@Composable
internal fun DocumentFooter(content: @Composable BoxScope.() -> Unit) {
    val report = LocalDocumentFooterHeight.current
    val density = LocalDensity.current
    DisposableEffect(report) { onDispose { report(null) } }
    Box(
        Modifier.fillMaxWidth().heightIn(min = documentFooterMinHeight())
            .onSizeChanged { report(with(density) { it.height.toDp() }) }
            .testTag("document.footer"),
        contentAlignment = Alignment.BottomStart,
        content = content,
    )
}
