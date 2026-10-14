package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import reframecv.domain.models.resume.content.ResumeSkillGroup
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_add_group
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_group
import reframecv.shared.generated.resources.resume_remove_skill_group
import reframecv.shared.generated.resources.resume_remove_skill_group_question
import reframecv.shared.generated.resources.resume_remove_skill_group_title
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.SkillGroupFields(
    groups: List<ResumeSkillGroup>,
    onChange: (List<ResumeSkillGroup>) -> Unit,
) {
    groups.forEachIndexed { index, group ->
        key(index, groups.size) {
            Column {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
                ) {
                    Field(group.title, Res.string.resume_group, {
                        onChange(groups.replacedAt(index, group.copy(title = it)))
                    })
                    CommaSeparatedItemsField(group.items) {
                        onChange(groups.replacedAt(index, group.copy(items = it)))
                    }
                }
                ConfirmedRemoveAction(
                    ConfirmationDialogContent(
                        title = Res.string.resume_remove_skill_group_title,
                        message = Res.string.resume_remove_skill_group_question,
                        confirmLabel = Res.string.resume_remove_skill_group,
                        cancelLabel = Res.string.resume_cancel,
                        subject = group.title.takeIf { it.isNotBlank() },
                        tone = ConfirmationDialogTone.Critical,
                    ),
                    onConfirm = { onChange(groups.removedAt(index)) },
                )
            }
        }
    }
    Action(Res.string.resume_add_group, { onChange(groups + ResumeSkillGroup()) })
}
