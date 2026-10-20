package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import reframecv.ui.components.common.confirmation.ConfirmationDialogState
import reframecv.ui.components.projects.resumes.editor.ResumeDeleteState
import reframecv.ui.components.projects.resumes.editor.ResumeEditorComponent
import reframecv.ui.compose.common.ConfirmationDialog

@Composable
internal fun ResumeDeleteConfirmation(component: ResumeEditorComponent) {
    val state by component.deleteState.subscribeAsState()
    val content = component.deleteDialogContent
    ConfirmationDialog(
        content = content,
        state = ConfirmationDialogState(
            confirmEnabled = component.canConfirmDelete,
            busy = state == ResumeDeleteState.Deleting,
            error = if (state == ResumeDeleteState.Failed) content.failureMessage else null,
        ),
        onAction = component::onDeleteDialogAction,
        modifier = Modifier.testTag("resume.delete.dialog"),
    )
}
