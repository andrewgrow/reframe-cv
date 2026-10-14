package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_remove
import reframecv.shared.generated.resources.resume_remove_section_question
import reframecv.shared.generated.resources.resume_remove_section_title
import reframecv.ui.components.common.confirmation.ConfirmationDialogAction
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogState
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.compose.common.ConfirmationDialog

@Composable
internal fun ResumeSectionRemoveConfirmation(
    sectionName: String,
    enabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmationDialog(
        content = ConfirmationDialogContent(
            title = Res.string.resume_remove_section_title,
            message = Res.string.resume_remove_section_question,
            subject = sectionName.takeIf { it.isNotBlank() },
            confirmLabel = Res.string.resume_remove,
            cancelLabel = Res.string.resume_cancel,
            tone = ConfirmationDialogTone.Critical,
        ),
        state = ConfirmationDialogState(confirmEnabled = enabled, busy = !enabled),
        onAction = { action ->
            when (action) {
                ConfirmationDialogAction.Back, ConfirmationDialogAction.Cancel -> onCancel()
                ConfirmationDialogAction.Confirm -> onConfirm()
            }
        },
        modifier = Modifier.testTag("resume.section.remove.dialog"),
    )
}
