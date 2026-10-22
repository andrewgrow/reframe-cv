package reframecv.ui.compose.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import kotlinx.coroutines.launch
import reframecv.ui.theme.ReframeTheme

/** Gives wide content the same draggable thumb and end controls as vertical forms. */
@Composable
internal fun FastHorizontalScrollRail(state: ScrollState) {
    val size = ReframeTheme.tokens.dimensions.scrollControlSize
    val scope = rememberCoroutineScope()
    Row(Modifier.fillMaxWidth().height(size)) {
        IconButton(
            { scope.launch { state.animateScrollTo(0) } },
            enabled = state.canScrollBackward,
            modifier = Modifier.size(size).testTag("scroll.to.left"),
        ) {
            Icon(
                Icons.Default.ArrowBack,
                "Scroll to left",
                Modifier.size(ReframeTheme.tokens.dimensions.iconSize),
            )
        }
        HorizontalTrack(state, Modifier.weight(1f).fillMaxHeight())
        IconButton(
            {
                scope.launch { state.animateScrollTo(state.maxValue) }
            },
            enabled = state.canScrollForward,
            modifier = Modifier.size(size).testTag("scroll.to.right"),
        ) {
            Icon(
                Icons.Default.ArrowForward,
                "Scroll to right",
                Modifier.size(ReframeTheme.tokens.dimensions.iconSize),
            )
        }
    }
}

@Composable
private fun HorizontalTrack(state: ScrollState, modifier: Modifier) {
    var width by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val minimum = with(density) { ReframeTheme.tokens.dimensions.scrollThumbMinHeight.toPx() }
    val thickness = with(density) { ReframeTheme.tokens.dimensions.scrollbarThickness.toPx() }
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val thumbColor = ReframeTheme.colorScheme.hint
    val scope = rememberCoroutineScope()
    val geometry = { scrollThumb(state, width.toFloat(), minimum) }
    Canvas(
        modifier.testTag("scroll.horizontal.track").onSizeChanged { width = it.width }
            .semantics {
                contentDescription = "Horizontal scroll position"
                progressBarRangeInfo =
                    ProgressBarRangeInfo(state.value.toFloat(), 0f..state.maxValue.toFloat())
                setProgress { value ->
                    scope.launch { state.scrollTo(value.toInt().coerceIn(0, state.maxValue)) }
                    true
                }
            }
            .pointerInput(state) {
                detectTapGestures { point ->
                    scope.launch {
                        state.scrollTo(geometry().scrollPosition(point.x - geometry().height / 2))
                    }
                }
            }
            .pointerInput(state) {
                var position = 0f
                var grab = 0f
                detectDragGestures(onDragStart = {
                    val thumb = geometry()
                    position = it.x
                    grab =
                        if (it.x in
                            thumb.top..thumb.top + thumb.height
                        ) {
                            it.x - thumb.top
                        } else {
                            thumb.height / 2
                        }
                }, onDrag = { change, amount ->
                    change.consume()
                    position += amount.x
                    val target = geometry().scrollPosition(position - grab)
                    state.dispatchRawDelta((target - state.value).toFloat())
                })
            },
    ) {
        val thumb = geometry()
        val top = (size.height - thickness) / 2
        val radius = CornerRadius(thickness / 2)
        drawRoundRect(track, Offset(0f, top), Size(size.width, thickness), radius)
        drawRoundRect(thumbColor, Offset(thumb.top, top), Size(thumb.height, thickness), radius)
    }
}
