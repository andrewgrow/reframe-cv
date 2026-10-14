package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.decodeToImageBitmap
import reframecv.ui.components.projects.resumes.editor.ResumePhotoState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumePhotoCropPreview(state: ResumePhotoState, showOutside: Boolean = false) {
    val image = remember(state.bytes) { state.bytes?.decodeToImageBitmap() } ?: return
    val margin = if (showOutside) ReframeTheme.tokens.spacing.medium else 0.dp
    val outline = ReframeTheme.colorScheme.hint
    val outlineWidth = ReframeTheme.tokens.outlineWidth
    Canvas(Modifier.fillMaxSize()) {
        val inset = margin.toPx()
        val width = size.width - inset * 2
        val height = size.height - inset * 2
        val placement = state.crop.placement(
            image.width.toFloat(),
            image.height.toFloat(),
            width,
            height,
        )
        clipRect {
            drawImage(
                image,
                dstOffset = IntOffset(
                    (inset + placement.left).roundToInt(),
                    (inset + placement.top).roundToInt(),
                ),
                dstSize = IntSize(placement.width.roundToInt(), placement.height.roundToInt()),
            )
        }
        if (showOutside) {
            val shade = Color.Black.copy(alpha = OUTSIDE_ALPHA)
            drawRect(shade, size = Size(size.width, inset))
            drawRect(shade, Offset(0f, inset + height), Size(size.width, inset))
            drawRect(shade, Offset(0f, inset), Size(inset, height))
            drawRect(shade, Offset(inset + width, inset), Size(inset, height))
            drawRect(
                outline,
                Offset(inset, inset),
                Size(width, height),
                style = Stroke(outlineWidth.toPx()),
            )
        }
    }
}

private const val OUTSIDE_ALPHA = 0.65f
