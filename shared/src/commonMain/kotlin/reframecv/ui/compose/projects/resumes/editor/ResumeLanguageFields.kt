package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import reframecv.domain.models.resume.content.ResumeLanguage
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_add_entry
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_language
import reframecv.shared.generated.resources.resume_level
import reframecv.shared.generated.resources.resume_remove_language
import reframecv.shared.generated.resources.resume_remove_language_question
import reframecv.shared.generated.resources.resume_remove_language_title
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.LanguageFields(
    entries: List<ResumeLanguage>,
    onChange: (List<ResumeLanguage>) -> Unit,
) {
    entries.forEachIndexed { index, entry ->
        key(index, entries.size) {
            Column {
                LanguageEntryFields(entry) { onChange(entries.replacedAt(index, it)) }
                ConfirmedRemoveAction(
                    ConfirmationDialogContent(
                        title = Res.string.resume_remove_language_title,
                        message = Res.string.resume_remove_language_question,
                        confirmLabel = Res.string.resume_remove_language,
                        cancelLabel = Res.string.resume_cancel,
                        subject = entry.language.takeIf { it.isNotBlank() },
                        tone = ConfirmationDialogTone.Critical,
                    ),
                    onConfirm = { onChange(entries.removedAt(index)) },
                )
            }
        }
    }
    Action(Res.string.resume_add_entry, { onChange(entries + ResumeLanguage()) })
}

@Composable
private fun ResumeFormScope.LanguageEntryFields(
    entry: ResumeLanguage,
    onChange: (ResumeLanguage) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        Box(Modifier.weight(1f)) {
            Field(entry.language, Res.string.resume_language, {
                onChange(entry.copy(language = it))
            })
        }
        Box(Modifier.weight(1f)) {
            Field(entry.level, Res.string.resume_level, { onChange(entry.copy(level = it)) })
        }
    }
}
