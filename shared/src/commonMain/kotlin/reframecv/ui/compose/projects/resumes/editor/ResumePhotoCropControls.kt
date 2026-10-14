package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import reframecv.photos.PhotoCrop
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_photo_down
import reframecv.shared.generated.resources.resume_photo_left
import reframecv.shared.generated.resources.resume_photo_reset
import reframecv.shared.generated.resources.resume_photo_right
import reframecv.shared.generated.resources.resume_photo_up
import reframecv.shared.generated.resources.resume_photo_zoom_in
import reframecv.shared.generated.resources.resume_photo_zoom_out
import reframecv.ui.components.projects.resumes.editor.ResumePhotoComponent
import reframecv.ui.components.projects.resumes.editor.ResumePhotoState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumePhotoCropControls(
    component: ResumePhotoComponent,
    state: ResumePhotoState,
    enabled: Boolean,
) {
    BoxWithConstraints {
        val inset = ReframeTheme.tokens.spacing.medium * 2
        val arrowWidth = ReframeTheme.tokens.dimensions.iconButtonSize.coerceAtLeast(48.dp)
        val width = minOf(
            ReframeTheme.tokens.dimensions.resumePhotoDialogWidth,
            (maxWidth - arrowWidth * 2 - inset).coerceAtLeast(1.dp),
        )
        PhotoFramingControls(component, state, enabled, width)
    }
}

@Composable
private fun PhotoFramingControls(
    component: ResumePhotoComponent,
    state: ResumePhotoState,
    enabled: Boolean,
    width: androidx.compose.ui.unit.Dp,
) {
    val crop = state.crop
    val inset = ReframeTheme.tokens.spacing.medium * 2
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            { component.onCropChange(crop.copy(y = crop.y - PhotoCrop.MOVE_STEP)) },
            enabled = enabled && crop.y > -1f,
        ) {
            Icon(Icons.Outlined.ArrowUpward, stringResource(Res.string.resume_photo_up))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                { component.onCropChange(crop.copy(x = crop.x - PhotoCrop.MOVE_STEP)) },
                enabled = enabled && crop.x > -1f,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    stringResource(Res.string.resume_photo_left),
                )
            }
            Box(Modifier.size(width + inset, width * PHOTO_HEIGHT_RATIO + inset)) {
                ResumePhotoCropPreview(state, showOutside = true)
                if (state.loading) {
                    CircularProgressIndicator(
                        Modifier.size(
                            ReframeTheme.tokens.dimensions.iconSize,
                        ).align(Alignment.Center),
                    )
                }
            }
            IconButton(
                { component.onCropChange(crop.copy(x = crop.x + PhotoCrop.MOVE_STEP)) },
                enabled = enabled && crop.x < 1f,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    stringResource(Res.string.resume_photo_right),
                )
            }
        }
        IconButton(
            { component.onCropChange(crop.copy(y = crop.y + PhotoCrop.MOVE_STEP)) },
            enabled = enabled && crop.y < 1f,
        ) {
            Icon(Icons.Outlined.ArrowDownward, stringResource(Res.string.resume_photo_down))
        }
        PhotoZoomControls(component, crop, enabled)
    }
}

@Composable
private fun PhotoZoomControls(component: ResumePhotoComponent, crop: PhotoCrop, enabled: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
        IconButton(
            {
                component.onCropChange(crop.copy(zoom = crop.zoom - PhotoCrop.ZOOM_STEP))
            },
            enabled = enabled && crop.zoom > 1f,
        ) {
            Icon(Icons.Outlined.Remove, stringResource(Res.string.resume_photo_zoom_out))
        }
        TextButton({ component.onCropChange(PhotoCrop()) }, enabled = enabled) {
            Text(stringResource(Res.string.resume_photo_reset))
        }
        IconButton(
            {
                component.onCropChange(crop.copy(zoom = crop.zoom + PhotoCrop.ZOOM_STEP))
            },
            enabled = enabled && crop.zoom < PhotoCrop.MAX_ZOOM,
        ) {
            Icon(Icons.Outlined.Add, stringResource(Res.string.resume_photo_zoom_in))
        }
    }
}

private const val PHOTO_HEIGHT_RATIO = 45f / 35f
