package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import reframecv.ui.components.common.confirmation.ConfirmationDialogAction
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogState
import reframecv.ui.compose.common.ConfirmationDialog

@Composable
internal fun ResumeFormScope.ConfirmedRemoveAction(
    content: ConfirmationDialogContent,
    onConfirm: () -> Unit,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Action(content.confirmLabel, { confirming = true })
    }
    if (confirming) {
        ConfirmationDialog(
            content,
            ConfirmationDialogState(confirmEnabled = enabled, busy = !enabled),
            onAction = { action ->
                when (action) {
                    ConfirmationDialogAction.Back, ConfirmationDialogAction.Cancel ->
                        confirming =
                            false

                    ConfirmationDialogAction.Confirm -> if (enabled) {
                        confirming = false
                        onConfirm()
                    }
                }
            },
            modifier = Modifier.testTag("resume.item.remove.dialog"),
        )
    }
}
