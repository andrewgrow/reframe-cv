package reframecv.ui.compose.projects.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.action_delete
import reframecv.ui.components.projects.editor.EditorComponent
import reframecv.ui.components.projects.editor.EditorDeleteState
import reframecv.ui.compose.common.ResizableDialog
import reframecv.ui.theme.ReframeTheme

/** Reuses the project editor's deletion confirmation from the empty project page. */
@Composable
internal fun ProjectDeletionDialog(component: EditorComponent) {
    val state by component.deleteState.subscribeAsState()
    val busy = state == EditorDeleteState.Deleting
    ResizableDialog(
        onDismissRequest = component::onClose,
        resizingEnabled = !busy,
        modifier = Modifier.testTag("project.delete.dialog"),
        title = { SelectableText(stringResource(Res.string.action_delete)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
            ) {
                SelectableText(component.initialName.orEmpty())
                DeleteConfirmation(component, busy, state)
            }
        },
        actions = {
            TextButton(onClick = component::onClose, enabled = !busy) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
