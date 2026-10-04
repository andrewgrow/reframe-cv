package reframecv.ui.compose.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import reframecv.ui.theme.ReframeTheme

object FillButton {
    enum class OutlineState {
        Visible,
        Hidden,
    }
}

/** A button whose background fills from left to right on hover, focus, or press. */
@Composable
@Suppress("LongParameterList")
fun FillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlineState: FillButton.OutlineState = FillButton.OutlineState.Hidden,
    containerColor: Color = Color.Transparent,
    fillColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    filledContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val highlighted = enabled && (hovered || focused || pressed)
    val fill by animateFloatAsState(
        targetValue = if (highlighted) 1f else 0f,
        animationSpec = tween(ReframeTheme.tokens.fillAnimationMillis),
    )
    val disabledColor = contentColor.copy(alpha = ReframeTheme.tokens.disabledContentAlpha)
    val textColor = if (enabled) lerp(contentColor, filledContentColor, fill) else disabledColor
    val border = when {
        !enabled -> BorderStroke(ReframeTheme.tokens.outlineWidth, disabledColor)

        outlineState == FillButton.OutlineState.Visible -> BorderStroke(
            ReframeTheme.tokens.outlineWidth,
            fillColor,
        )

        else -> null
    }

    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        Button(
            onClick = onClick,
            enabled = enabled,
            elevation = null,
            contentPadding = ReframeTheme.tokens.buttonContentPadding,
            border = border,
            modifier = modifier.height(
                ReframeTheme.tokens.dimensions.buttonHeight,
            ).clip(ButtonDefaults.shape).drawBehind {
                drawRect(color = containerColor)
                drawRect(color = fillColor, size = Size(size.width * fill, size.height))
            },
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = textColor,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = disabledColor,
            ),
        ) {
            Text(text, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
