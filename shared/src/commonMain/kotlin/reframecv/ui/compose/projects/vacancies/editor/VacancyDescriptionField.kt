package reframecv.ui.compose.projects.vacancies.editor

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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_description
import reframecv.shared.generated.resources.vacancy_resize_description
import reframecv.ui.compose.common.ResizeHandle
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    scroll: ScrollState,
    viewport: Rect,
) {
    val minimum = ReframeTheme.tokens.dimensions.vacancyDescriptionMinHeight
    val initial = ReframeTheme.tokens.dimensions.vacancyDescriptionDefaultHeight
    val state = rememberSaveable(saver = DescriptionResizeState.saver) {
        DescriptionResizeState(initial.value)
    }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(state.height.coerceAtLeast(minimum.value).dp)
                .testTag("vacancy.description"),
            label = { Text(stringResource(Res.string.vacancy_description)) },
            enabled = enabled,
            minLines = 2,
        )
        DescriptionResizeHandle(state, enabled, Modifier.align(Alignment.BottomEnd))
    }
    DescriptionResizeAutoScroll(state, scroll, viewport, enabled)
}

@Composable
private fun DescriptionResizeHandle(
    state: DescriptionResizeState,
    enabled: Boolean,
    modifier: Modifier,
) {
    val density = LocalDensity.current.density
    val minimum = ReframeTheme.tokens.dimensions.vacancyDescriptionMinHeight.value
    ResizeHandle(
        label = stringResource(Res.string.vacancy_resize_description),
        enabled = enabled,
        modifier = modifier.testTag("vacancy.description.resize"),
        onDrag = { delta, position ->
            state.resize(delta.y, density, minimum)
            state.pointerY = position.y
        },
        onDragEnd = { state.pointerY = Float.NaN },
    )
}

@Composable
private fun DescriptionResizeAutoScroll(
    state: DescriptionResizeState,
    scroll: ScrollState,
    viewport: Rect,
    enabled: Boolean,
) {
    val density = LocalDensity.current.density
    val minimum = ReframeTheme.tokens.dimensions.vacancyDescriptionMinHeight.value
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
