package reframecv.domain.models.settings

import reframecv.domain.DomainModel

/** Keeps device preferences consistent across projects and application launches. */
data class AppSettings(val window: WindowSettings = WindowSettings()) : DomainModel

/** Describes the window layout the user expects to see on the next launch. */
data class WindowSettings(
    val width: Float = 800f,
    val height: Float = 600f,
    val maximized: Boolean = false,
) {
    init {
        require(width.isFinite() && width > 0f) { "Window width must be finite and positive" }
        require(height.isFinite() && height > 0f) { "Window height must be finite and positive" }
    }
}
