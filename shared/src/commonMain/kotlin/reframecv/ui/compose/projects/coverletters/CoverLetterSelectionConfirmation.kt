package reframecv.ui.compose.projects.coverletters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.cover_letters_delete_error
import reframecv.shared.generated.resources.cover_letters_delete_title
import reframecv.shared.generated.resources.cover_letters_delete_warning
import reframecv.shared.generated.resources.cover_letters_selected
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_delete_confirm
import reframecv.ui.components.common.confirmation.ConfirmationDialogAction
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogState
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.components.projects.coverletters.CoverLettersComponent
import reframecv.ui.components.projects.coverletters.CoverLettersState
import reframecv.ui.compose.common.ConfirmationDialog

@Composable
internal fun CoverLetterSelectionConfirmation(component: CoverLettersComponent) {
    val selection by component.selection.state.subscribeAsState()
    val records by component.uiState.subscribeAsState()
    if (!selection.confirming) return
    val names = (records as? CoverLettersState.Ready)?.letters.orEmpty().filter {
        it.id in
            selection.selected
    }.joinToString("\n") { it.name }
    ConfirmationDialog(
        ConfirmationDialogContent(
            Res.string.cover_letters_delete_title,
            Res.string.cover_letters_delete_warning,
            Res.string.resume_delete_confirm,
            Res.string.resume_cancel,
            subject =
                stringResource(Res.string.cover_letters_selected, selection.selected.size) + "\n" +
                    names,
            tone = ConfirmationDialogTone.Critical,
        ),
        ConfirmationDialogState(
            busy = selection.deleting,
            confirmEnabled = selection.selected.isNotEmpty(),
            error = if (selection.failed) Res.string.cover_letters_delete_error else null,
        ),
        {
            if (it ==
                ConfirmationDialogAction.Confirm
            ) {
                component.selection.onConfirmDelete()
            } else {
                component.selection.onCancelDelete()
            }
        },
        modifier = Modifier.testTag("coverletters.delete.dialog"),
    )
}
