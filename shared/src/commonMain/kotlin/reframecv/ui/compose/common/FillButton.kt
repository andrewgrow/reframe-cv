package reframecv.ui.compose.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val HOVER_ANIMATION_DURATION = 300
private const val FILL_BUTTON_HEIGHT = 48
private const val DISABLED_CONTENT_ALPHA = 0.38f

/** A button whose background fills from left to right on hover, focus, or press. */
@Composable
@Suppress("LongParameterList")
fun FillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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
        animationSpec = tween(HOVER_ANIMATION_DURATION),
    )
    val textColor by animateColorAsState(
        targetValue = if (highlighted) {
            filledContentColor
        } else {
            contentColor
        },
        animationSpec = tween(HOVER_ANIMATION_DURATION),
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(
            FILL_BUTTON_HEIGHT.dp,
        ).clip(ButtonDefaults.shape).drawBehind {
            drawRect(color = containerColor)
            drawRect(color = fillColor, size = Size(size.width * fill, size.height))
        },
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = textColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = contentColor.copy(alpha = DISABLED_CONTENT_ALPHA),
        ),
    ) {
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
