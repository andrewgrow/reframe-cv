package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_keywords
import reframecv.shared.generated.resources.resume_keywords_hint
import reframecv.shared.generated.resources.resume_name
import reframecv.shared.generated.resources.resume_name_hint
import reframecv.shared.generated.resources.resume_name_required
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.NameAndKeywordsFields(
    draft: ResumeDraft,
    onChange: (ResumeDraft) -> Unit,
    focus: FocusRequester,
    nameError: Boolean,
) {
    val tokens = ReframeTheme.tokens
    BoxWithConstraints(Modifier.testTag("resume.identity")) {
        if (maxWidth >= tokens.dimensions.resumeNameKeywordsMinWidth) {
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium)) {
                Box(Modifier.weight(2f)) { NameField(draft, onChange, focus, nameError) }
                Box(Modifier.weight(1f)) { KeywordsField(draft, onChange) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium)) {
                NameField(draft, onChange, focus, nameError)
                KeywordsField(draft, onChange)
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
            error = if (nameError) stringResource(Res.string.resume_name_required) else null,
        )
        SelectableText(
            stringResource(Res.string.resume_name_hint),
            color = ReframeTheme.colorScheme.hint,
            style = ReframeTheme.tokens.typography.bodySmall,
        )
    }
}

@Composable
private fun ResumeFormScope.KeywordsField(draft: ResumeDraft, onChange: (ResumeDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
        Field(draft.keywords, Res.string.resume_keywords, {
            onChange(draft.copy(keywords = it))
        }, tag = "resume.keywords")
        SelectableText(
            stringResource(Res.string.resume_keywords_hint),
            color = ReframeTheme.colorScheme.hint,
            style = ReframeTheme.tokens.typography.bodySmall,
        )
    }
}
