package reframecv.ui.compose.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResizeHandle(
    label: String,
    enabled: Boolean,
    onDrag: (delta: Offset, position: Offset) -> Unit,
    modifier: Modifier = Modifier,
    onDragEnd: () -> Unit = {},
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val dragCallback by rememberUpdatedState(onDrag)
    val endCallback by rememberUpdatedState(onDragEnd)
    val color = ReframeTheme.colorScheme.hint
        .let { if (enabled) it else it.copy(alpha = ReframeTheme.tokens.disabledContentAlpha) }
    Canvas(
        modifier.size(ReframeTheme.tokens.dimensions.iconSize)
            .semantics {
                contentDescription = label
                if (!enabled) disabled()
            }
            .pointerHoverIcon(if (enabled) PointerIcon.Crosshair else PointerIcon.Default)
            .onGloballyPositioned { coordinates = it }
            .pointerInput(enabled) {
                if (enabled) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        var previous = coordinates?.localToWindow(down.position)
                        try {
                            drag(down.id) { change ->
                                change.consume()
                                val position = coordinates?.localToWindow(change.position)
                                val origin = previous
                                if (position != null && origin != null) {
                                    dragCallback(position - origin, position)
                                    previous = position
                                }
                            }
                        } finally {
                            endCallback()
                        }
                    }
                }
            },
    ) {
        val inset = size.width / 4
        for (length in listOf(inset, inset * 2)) {
            drawLine(
                color,
                Offset(size.width - inset - length, size.height - inset),
                Offset(size.width - inset, size.height - inset - length),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}
