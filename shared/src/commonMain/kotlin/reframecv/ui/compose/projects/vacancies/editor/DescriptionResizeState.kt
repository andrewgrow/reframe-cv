package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue

/** Lets users expand long descriptions without losing the chosen field height. */
internal class DescriptionResizeState(initialHeight: Float) {
    var height by mutableFloatStateOf(initialHeight)
        private set
    var pointerY by mutableFloatStateOf(Float.NaN)

    fun resize(pixels: Float, density: Float, minimum: Float): Float {
        val previous = height
        height = (height + pixels / density).coerceAtLeast(minimum)
        return (height - previous) * density
    }

    companion object {
        val saver = Saver<DescriptionResizeState, Float>(
            save = { it.height },
            restore = { DescriptionResizeState(it) },
        )
    }
}
