package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_name
import reframecv.shared.generated.resources.resume_name_hint
import reframecv.shared.generated.resources.resume_name_required
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumePhotoComponent
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.NameAndPhotoFields(
    draft: ResumeDraft,
    onChange: (ResumeDraft) -> Unit,
    focus: FocusRequester,
    nameError: Boolean,
    photoComponent: ResumePhotoComponent,
) {
    val tokens = ReframeTheme.tokens
    BoxWithConstraints {
        if (maxWidth >= tokens.dimensions.resumeHeaderMinWidth) {
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.large)) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.large),
                ) {
                    NameField(draft, onChange, focus, nameError)
                    MetadataFields(draft, onChange)
                }
                PhotoFields(draft, onChange, photoComponent)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.large)) {
                NameField(draft, onChange, focus, nameError)
                MetadataFields(draft, onChange)
                Column(Modifier.align(Alignment.CenterHorizontally)) {
                    PhotoFields(draft, onChange, photoComponent)
                }
            }
        }
    }
}

@Composable
private fun ResumeFormScope.NameField(
    draft: ResumeDraft,
    onChange: (ResumeDraft) -> Unit,
    focus: FocusRequester,
    nameError: Boolean,
) {
    Column(
        Modifier.focusRequester(focus),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        Field(
            draft.name,
            Res.string.resume_name,
            { onChange(draft.copy(name = it)) },
            tag = "resume.name",
            error = if (nameError) {
                stringResource(
                    Res.string.resume_name_required,
                )
            } else {
                null
            },
        )
        SelectableText(
            stringResource(Res.string.resume_name_hint),
            color = ReframeTheme.colorScheme.hint,
            style = ReframeTheme.tokens.typography.bodySmall,
        )
    }
}

@Composable
private fun ResumeFormScope.PhotoFields(
    draft: ResumeDraft,
    onChange: (ResumeDraft) -> Unit,
    component: ResumePhotoComponent,
) {
    Column(
        Modifier.width(ReframeTheme.tokens.dimensions.resumePhotoWidth),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        ResumePhotoBlock(component, enabled)
        ResumePhotoVisibility(draft.photoVisible, enabled) {
            onChange(draft.copy(photoVisible = it))
        }
    }
}
