package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/** Lets users resize the vacancy editor while keeping it usable within the window. */
internal class DialogResizeState {
    var width by mutableFloatStateOf(0f)
        private set
    var height by mutableFloatStateOf(0f)
        private set
    var resized by mutableStateOf(false)
        private set
    private var minimumWidth = 0f
    private var minimumHeight = 0f

    fun captureMinimum(size: IntSize, density: Float) {
        if (minimumWidth == 0f && size.width > 0 && size.height > 0) {
            minimumWidth = size.width / density
            minimumHeight = size.height / density
        }
        if (!resized) {
            width = size.width / density
            height = size.height / density
        }
    }

    fun resize(delta: Offset, density: Float, window: IntSize) {
        if (minimumWidth == 0f || window.width == 0 || window.height == 0) return
        val maximumWidth = window.width / density
        val maximumHeight = window.height / density
        // A centered dialog's dragged corner moves by half the change in its size.
        width = (width + delta.x * 2 / density)
            .coerceIn(minimumWidth.coerceAtMost(maximumWidth), maximumWidth)
        height = (height + delta.y * 2 / density)
            .coerceIn(minimumHeight.coerceAtMost(maximumHeight), maximumHeight)
        resized = true
    }

    companion object {
        val saver = listSaver(
            save = {
                listOf(
                    it.width,
                    it.height,
                    it.minimumWidth,
                    it.minimumHeight,
                    if (it.resized) 1f else 0f,
                )
            },
            restore = {
                DialogResizeState().apply {
                    width = it[0]
                    height = it[1]
                    minimumWidth = it[2]
                    minimumHeight = it[3]
                    resized = it[4] == 1f
                }
            },
        )
    }
}
