package reframecv.domain.models.settings

import reframecv.domain.DomainModel

/** Device-local application preferences, independent of individual projects. */
data class AppSettings(val window: WindowSettings = WindowSettings()) : DomainModel

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
