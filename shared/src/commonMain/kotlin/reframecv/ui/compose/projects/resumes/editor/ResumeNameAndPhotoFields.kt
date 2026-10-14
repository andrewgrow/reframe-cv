package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
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
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium)) {
        Column(
            Modifier.weight(1f).focusRequester(focus),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
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
                style = tokens.typography.bodySmall,
            )
        }
        ResumePhotoBlock(photoComponent, enabled)
    }
}
