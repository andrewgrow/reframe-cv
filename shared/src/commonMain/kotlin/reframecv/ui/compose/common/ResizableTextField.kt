package reframecv.ui.compose.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import reframecv.ui.theme.ReframeTheme

/** Lets multiline text fields grow vertically and scroll their surrounding content during dragging. */
@Suppress("LongParameterList")
@Composable
internal fun ResizableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    scroll: ScrollState,
    viewport: Rect,
    label: String,
    resizeLabel: String,
    tag: String,
    minimumHeight: Dp,
    initialHeight: Dp = minimumHeight,
    isError: Boolean = false,
) {
    val state = rememberSaveable(saver = TextFieldResizeState.saver) {
        TextFieldResizeState(initialHeight.value)
    }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(
                state.height.coerceAtLeast(minimumHeight.value).dp,
            )
                .testTag(tag),
            label = { Text(label) },
            enabled = enabled,
            minLines = 2,
            isError = isError,
        )
        TextFieldResizeHandle(
            state,
            enabled,
            minimumHeight,
            resizeLabel,
            Modifier.align(Alignment.BottomEnd).testTag("$tag.resize"),
        )
    }
    TextFieldResizeAutoScroll(state, scroll, viewport, enabled, minimumHeight)
}

@Composable
private fun TextFieldResizeHandle(
    state: TextFieldResizeState,
    enabled: Boolean,
    minimumHeight: Dp,
    label: String,
    modifier: Modifier,
) {
    val density = LocalDensity.current.density
    val minimum = minimumHeight.value
    ResizeHandle(
        label = label,
        enabled = enabled,
        modifier = modifier,
        onDrag = { delta, position ->
            state.resize(delta.y, density, minimum)
            state.pointerY = position.y
        },
        onDragEnd = { state.pointerY = Float.NaN },
    )
}

@Composable
private fun TextFieldResizeAutoScroll(
    state: TextFieldResizeState,
    scroll: ScrollState,
    viewport: Rect,
    enabled: Boolean,
    minimumHeight: Dp,
) {
    val density = LocalDensity.current.density
    val minimum = minimumHeight.value
    val edge = with(LocalDensity.current) { ReframeTheme.tokens.dimensions.iconSize.toPx() }
    val bounds by rememberUpdatedState(viewport)
    LaunchedEffect(state.pointerY.isFinite(), enabled) {
        while (enabled && state.pointerY.isFinite()) {
            withFrameNanos { }
            val step = when {
                bounds.height <= 0 -> 0f
                state.pointerY > bounds.bottom - edge -> edge / 4
                state.pointerY < bounds.top + edge -> -edge / 4
                else -> 0f
            }
            if (step != 0f) {
                val resized = state.resize(step, density, minimum)
                // Lay out the expanded field before scrolling into the newly available space.
                withFrameNanos { }
                scroll.scrollBy(resized)
            }
        }
    }
}
