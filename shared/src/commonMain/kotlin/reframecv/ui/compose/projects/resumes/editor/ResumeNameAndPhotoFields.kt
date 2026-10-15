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
                    NameAndKeywordsFields(draft, onChange, focus, nameError)
                    MetadataFields(draft, onChange)
                }
                PhotoFields(draft, onChange, photoComponent)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.large)) {
                NameAndKeywordsFields(draft, onChange, focus, nameError)
                MetadataFields(draft, onChange)
                Column(Modifier.align(Alignment.CenterHorizontally)) {
                    PhotoFields(draft, onChange, photoComponent)
                }
            }
        }
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
