package reframecv.ui.compose.projects.coverletters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.letter_delete_error
import reframecv.shared.generated.resources.letter_delete_title
import reframecv.shared.generated.resources.letter_delete_warning
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_delete_confirm
import reframecv.ui.components.common.confirmation.ConfirmationDialogAction
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogState
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.components.projects.coverletters.CoverLetterEditorComponent
import reframecv.ui.components.projects.coverletters.LetterNotice
import reframecv.ui.compose.common.ConfirmationDialog

@Composable
internal fun CoverLetterDeleteDialog(component: CoverLetterEditorComponent) {
    val state by component.state.subscribeAsState()
    if (!state.confirming) return
    ConfirmationDialog(
        ConfirmationDialogContent(
            Res.string.letter_delete_title,
            Res.string.letter_delete_warning,
            Res.string.resume_delete_confirm,
            Res.string.resume_cancel,
            subject = state.record.name,
            tone = ConfirmationDialogTone.Critical,
        ),
        ConfirmationDialogState(
            busy = state.busy,
            error = if (state.notice ==
                LetterNotice.DeleteFailed
            ) {
                Res.string.letter_delete_error
            } else {
                null
            },
        ),
        {
            if (it ==
                ConfirmationDialogAction.Confirm
            ) {
                component.onDelete()
            } else {
                component.onCancelDelete()
            }
        },
    )
}
