package reframecv.window

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import reframecv.domain.models.settings.WindowSettings

internal fun WindowSettings.fitToScreen(width: Float, height: Float) = copy(
    width = this.width.coerceAtMost(width),
    height = this.height.coerceAtMost(height),
)

internal fun WindowSettings.capture(
    size: DpSize,
    placement: WindowPlacement,
    minimized: Boolean,
): WindowSettings {
    if (minimized || placement == WindowPlacement.Fullscreen) return this
    return if (placement == WindowPlacement.Maximized) {
        copy(maximized = true)
    } else if (size.width.value.isValidDimension() && size.height.value.isValidDimension()) {
        copy(width = size.width.value, height = size.height.value, maximized = false)
    } else {
        this
    }
}

internal val WindowSettings.size: DpSize get() = DpSize(width.dp, height.dp)

private fun Float.isValidDimension() = isFinite() && this > 0f
