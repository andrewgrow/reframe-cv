package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.runtime.Composable
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_remove_position
import reframecv.shared.generated.resources.resume_remove_position_question
import reframecv.shared.generated.resources.resume_remove_position_title
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.components.projects.resumes.editor.ResumeSectionDraft

@Composable
internal fun ResumeFormScope.PositionRemoveAction(
    draft: ResumeSectionDraft,
    index: Int,
    onChange: (ResumeSectionDraft) -> Unit,
) {
    val section = draft.value as ResumeExperienceSection
    val position = section.entries[index]
    ConfirmedRemoveAction(
        ConfirmationDialogContent(
            title = Res.string.resume_remove_position_title,
            message = Res.string.resume_remove_position_question,
            confirmLabel = Res.string.resume_remove_position,
            cancelLabel = Res.string.resume_cancel,
            subject = listOf(position.position, position.company).filter { it.isNotBlank() }
                .joinToString(" — ").takeIf { it.isNotBlank() },
            tone = ConfirmationDialogTone.Critical,
        ),
    ) {
        onChange(
            draft.copy(
                value = section.copy(entries = section.entries.removedAt(index)),
                periods = draft.periods.removedAt(index),
            ),
        )
    }
}
