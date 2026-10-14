package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_close
import reframecv.shared.generated.resources.resume_photo
import reframecv.shared.generated.resources.resume_photo_choose
import reframecv.shared.generated.resources.resume_photo_delete_confirm
import reframecv.shared.generated.resources.resume_photo_delete_question
import reframecv.shared.generated.resources.resume_photo_error
import reframecv.shared.generated.resources.resume_photo_hint
import reframecv.shared.generated.resources.resume_photo_missing
import reframecv.shared.generated.resources.resume_photo_unavailable
import reframecv.ui.components.projects.resumes.editor.ResumePhotoComponent
import reframecv.ui.components.projects.resumes.editor.ResumePhotoState
import reframecv.ui.compose.common.ResizableDialog
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumePhotoBlock(component: ResumePhotoComponent, enabled: Boolean) {
    val state by component.state.subscribeAsState()
    val tokens = ReframeTheme.tokens
    Surface(
        onClick = component::onOpen,
        enabled = enabled,
        modifier = Modifier.width(tokens.dimensions.resumePhotoWidth).aspectRatio(PHOTO_ASPECT)
            .testTag("resume.photo"),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(tokens.outlineWidth, ReframeTheme.colorScheme.hint),
    ) {
        PhotoPreview(state)
    }
}

@Composable
private fun PhotoPreview(state: ResumePhotoState) {
    val image = remember(state.bytes) { state.bytes?.decodeToImageBitmap() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(
                image,
                stringResource(Res.string.resume_photo),
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Person,
                    null,
                    Modifier.size(ReframeTheme.tokens.dimensions.loadingIndicatorSize),
                )
                Text(
                    stringResource(
                        if (state.path != null &&
                            !state.loading
                        ) {
                            Res.string.resume_photo_unavailable
                        } else {
                            Res.string.resume_photo
                        },
                    ),
                    style = ReframeTheme.tokens.typography.bodySmall,
                )
            }
        }
        if (state.loading) {
            CircularProgressIndicator(
                Modifier.size(ReframeTheme.tokens.dimensions.iconSize),
            )
        }
    }
}

@Composable
internal fun ResumePhotoDialog(component: ResumePhotoComponent) {
    val state by component.state.subscribeAsState()
    val confirming by component.confirmingDelete.subscribeAsState()
    ResizableDialog(
        onDismissRequest = component::onClose,
        resizingEnabled = !state.loading,
        modifier = Modifier.testTag("resume.photo.dialog"),
        title = { Text(stringResource(Res.string.resume_photo)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                Box(
                    Modifier.width(ReframeTheme.tokens.dimensions.resumePhotoDialogWidth)
                        .aspectRatio(PHOTO_ASPECT).align(Alignment.CenterHorizontally),
                ) { PhotoPreview(state) }
                Text(stringResource(Res.string.resume_photo_hint))
                if (state.failed) {
                    Text(
                        stringResource(Res.string.resume_photo_error),
                        color = ReframeTheme.colorScheme.critical,
                    )
                } else if (state.path != null && state.bytes == null && !state.loading) {
                    Text(
                        stringResource(Res.string.resume_photo_missing),
                        color = ReframeTheme.colorScheme.critical,
                    )
                }
                AnimatedVisibility(confirming) {
                    Text(
                        stringResource(Res.string.resume_photo_delete_question),
                        color = ReframeTheme.colorScheme.critical,
                    )
                }
            }
        },
        actions = { PhotoDialogActions(component, state, confirming) },
    )
}

@Composable
private fun PhotoDialogActions(
    component: ResumePhotoComponent,
    state: ResumePhotoState,
    confirming: Boolean,
) {
    val deleteLabel = if (confirming) {
        Res.string.resume_photo_delete_confirm
    } else {
        Res.string.action_delete
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (state.path != null) {
            TextButton(
                if (confirming) component::onConfirmDelete else component::onRequestDelete,
                enabled = !state.loading,
            ) {
                Text(
                    stringResource(deleteLabel),
                    color = ReframeTheme.colorScheme.critical,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (confirming) {
            TextButton(component::onCancelDelete) { Text(stringResource(Res.string.resume_cancel)) }
        } else {
            TextButton(component::onChoose, enabled = !state.loading) {
                Text(stringResource(Res.string.resume_photo_choose))
            }
            TextButton(component::onClose, enabled = !state.loading) {
                Text(stringResource(Res.string.resume_close))
            }
        }
    }
}

private const val PHOTO_ASPECT = 35f / 45f
