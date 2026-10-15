package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_document_language
import reframecv.shared.generated.resources.resume_document_language_hint
import reframecv.shared.generated.resources.resume_keywords
import reframecv.shared.generated.resources.resume_main_hint
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.MetadataFields(draft: ResumeDraft, onChange: (ResumeDraft) -> Unit) {
    val tokens = ReframeTheme.tokens
    Column(
        Modifier.fillMaxWidth().testTag("resume.metadata"),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            ) {
                Box(Modifier.align(Alignment.CenterVertically)) { LanguageField(draft, onChange) }
                Box(Modifier.align(Alignment.CenterVertically)) {
                    RoleField(draft.role) { onChange(draft.copy(role = it)) }
                }
            }
            SelectableText(
                stringResource(Res.string.resume_main_hint),
                color = ReframeTheme.colorScheme.hint,
                style = tokens.typography.bodySmall,
            )
        }
        KeywordsField(draft, onChange)
    }
}

@Composable
private fun ResumeFormScope.LanguageField(draft: ResumeDraft, onChange: (ResumeDraft) -> Unit) {
    Box(Modifier.width(ReframeTheme.tokens.dimensions.resumeLanguageWidth)) {
        Field(draft.language, Res.string.resume_document_language, {
            onChange(draft.copy(language = it))
        }, tag = "resume.language", placeholder = Res.string.resume_document_language_hint)
    }
}

@Composable
private fun ResumeFormScope.KeywordsField(draft: ResumeDraft, onChange: (ResumeDraft) -> Unit) {
    Field(draft.keywords, Res.string.resume_keywords, {
        onChange(draft.copy(keywords = it))
    }, tag = "resume.keywords")
}
