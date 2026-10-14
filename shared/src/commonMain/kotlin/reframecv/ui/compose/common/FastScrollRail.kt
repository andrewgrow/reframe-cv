package reframecv.ui.compose.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.scroll_position
import reframecv.shared.generated.resources.scroll_to_bottom
import reframecv.shared.generated.resources.scroll_to_top
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun FastScrollRail(state: ScrollState) {
    val tokens = ReframeTheme.tokens
    val scope = rememberCoroutineScope()
    Column(
        Modifier.width(tokens.dimensions.scrollControlSize).fillMaxHeight()
            .scrollable(state, Orientation.Vertical, reverseDirection = true),
    ) {
        IconButton(
            onClick = { scope.launch { state.animateScrollTo(0) } },
            enabled = state.canScrollBackward,
            modifier = Modifier.size(tokens.dimensions.scrollControlSize).testTag("scroll.to.top"),
        ) {
            Icon(
                Icons.Default.VerticalAlignTop,
                stringResource(Res.string.scroll_to_top),
                Modifier.size(tokens.dimensions.iconSize),
            )
        }
        ScrollTrack(state, Modifier.weight(1f).fillMaxWidth())
        IconButton(
            onClick = { scope.launch { state.animateScrollTo(state.maxValue) } },
            enabled = state.canScrollForward,
            modifier = Modifier.size(
                tokens.dimensions.scrollControlSize,
            ).testTag("scroll.to.bottom"),
        ) {
            Icon(
                Icons.Default.VerticalAlignBottom,
                stringResource(Res.string.scroll_to_bottom),
                Modifier.size(tokens.dimensions.iconSize),
            )
        }
    }
}

@Composable
private fun ScrollTrack(state: ScrollState, modifier: Modifier) {
    val tokens = ReframeTheme.tokens
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var height by remember { mutableIntStateOf(0) }
    val minimum = with(density) { tokens.dimensions.scrollThumbMinHeight.toPx() }
    val thickness = with(density) { tokens.dimensions.scrollbarThickness.toPx() }
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val thumbColor = ReframeTheme.colorScheme.hint
    val description = stringResource(Res.string.scroll_position)
    val geometry = { scrollThumb(state, height.toFloat(), minimum) }
    Canvas(
        modifier.testTag("scroll.track").onSizeChanged { height = it.height }
            .semantics {
                contentDescription = description
                progressBarRangeInfo =
                    ProgressBarRangeInfo(state.value.toFloat(), 0f..state.maxValue.toFloat())
                setProgress { target ->
                    scope.launch { state.scrollTo(target.toInt().coerceIn(0, state.maxValue)) }
                    true
                }
            }
            .scrollTrackGestures(state, minimum, scope, geometry),
    ) {
        val thumb = geometry()
        val left = (size.width - thickness) / 2
        val radius = CornerRadius(thickness / 2)
        drawRoundRect(trackColor, Offset(left, 0f), Size(thickness, size.height), radius)
        drawRoundRect(thumbColor, Offset(left, thumb.top), Size(thickness, thumb.height), radius)
    }
}

private fun Modifier.scrollTrackGestures(
    state: ScrollState,
    minimum: Float,
    scope: CoroutineScope,
    geometry: () -> ScrollThumb,
): Modifier = this
    .pointerInput(state, minimum) {
        detectTapGestures { offset ->
            val thumb = geometry()
            if (offset.y !in thumb.top..(thumb.top + thumb.height)) {
                scope.launch {
                    state.scrollTo(thumb.scrollPosition(offset.y - thumb.height / 2))
                }
            }
        }
    }
    .pointerInput(state, minimum) {
        var position = 0f
        var grab = 0f
        detectDragGestures(
            onDragStart = { offset ->
                scope.launch { state.stopScroll() }
                val thumb = geometry()
                position = offset.y
                grab = if (offset.y in thumb.top..(thumb.top + thumb.height)) {
                    offset.y - thumb.top
                } else {
                    thumb.height / 2
                }
            },
            onDrag = { change, amount ->
                change.consume()
                position += amount.y
                val target = geometry().scrollPosition(position - grab)
                state.dispatchRawDelta((target - state.value).toFloat())
            },
        )
    }

/** Maps scroll offsets to thumb geometry, including short content and tiny viewports. */
internal data class ScrollThumb(
    val top: Float,
    val height: Float,
    val travel: Float,
    val maximum: Int,
) {
    fun scrollPosition(top: Float): Int = if (travel > 0f) {
        ((top / travel).coerceIn(0f, 1f) * maximum).toInt()
    } else {
        0
    }
}

internal fun scrollThumb(state: ScrollState, height: Float, minimum: Float): ScrollThumb {
    val maximum = state.maxValue.takeIf { it != Int.MAX_VALUE } ?: 0
    val content = state.viewportSize.toFloat() + maximum
    val thumbHeight = if (content > 0f) {
        (height * state.viewportSize / content).coerceIn(minimum.coerceAtMost(height), height)
    } else {
        height
    }
    val travel = height - thumbHeight
    val top = if (maximum > 0) travel * state.value / maximum else 0f
    return ScrollThumb(top, thumbHeight, travel, maximum)
}
