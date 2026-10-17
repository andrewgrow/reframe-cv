package reframecv.photos

import kotlin.math.max

/** Describes the portrait framing independently of preview and print dimensions. */
data class PhotoCrop(val zoom: Float = 1f, val x: Float = 0f, val y: Float = 0f) {
    fun constrained() = PhotoCrop(
        zoom = zoom.takeIf { it.isFinite() }?.coerceIn(1f, MAX_ZOOM) ?: 1f,
        x = x.takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f,
        y = y.takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f,
    )

    fun placement(
        imageWidth: Float,
        imageHeight: Float,
        width: Float,
        height: Float,
    ): PhotoPlacement {
        val crop = constrained()
        val scale = max(width / imageWidth, height / imageHeight) * crop.zoom
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        return PhotoPlacement(
            scaledWidth,
            scaledHeight,
            (width - scaledWidth) / 2 * (1f - crop.x),
            (height - scaledHeight) / 2 * (1f - crop.y),
        )
    }

    companion object {
        const val MAX_ZOOM = 4f
        const val ZOOM_STEP = 0.1f
        const val MOVE_STEP = 0.1f
    }
}

/** Supplies identical image placement to the crop preview and saved portrait. */
data class PhotoPlacement(val width: Float, val height: Float, val left: Float, val top: Float)
