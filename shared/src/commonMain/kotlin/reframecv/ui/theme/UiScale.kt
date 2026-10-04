package reframecv.ui.theme

import androidx.compose.runtime.Immutable

/** Application scale, independent of the operating system's display and font settings. */
@Immutable
data class UiScale(val percent: Int = DEFAULT_PERCENT) {
    init {
        require(percent in MIN_PERCENT..MAX_PERCENT) { "UI scale must be between 75% and 150%" }
    }

    val factor: Float get() = percent.toFloat() / DEFAULT_PERCENT

    fun smaller(): UiScale = UiScale((percent - STEP_PERCENT).coerceAtLeast(MIN_PERCENT))
    fun larger(): UiScale = UiScale((percent + STEP_PERCENT).coerceAtMost(MAX_PERCENT))

    companion object {
        const val DEFAULT_PERCENT = 100
        const val MIN_PERCENT = 75
        const val MAX_PERCENT = 150
        private const val STEP_PERCENT = 5
        val Default = UiScale()
    }
}
