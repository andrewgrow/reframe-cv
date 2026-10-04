package reframecv.ui.compose.projects.editor

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.project_editor_create_title
import reframecv.shared.generated.resources.project_editor_edit_title
import reframecv.ui.components.projects.editor.EditorComponent
import reframecv.ui.components.projects.editor.EditorDeleteState
import reframecv.ui.components.projects.editor.EditorSaveState

internal const val PROJECT_EDITOR_TAG = "projects.editor"

@Composable
fun EditorContent(
    component: EditorComponent,
    initialName: String? = component.initialName,
    onSave: (String) -> Unit = component::onSave,
) {
    val saveState by component.saveState.subscribeAsState()
    val deleteState by component.deleteState.subscribeAsState()
    val isBusy = saveState == EditorSaveState.Saving || deleteState == EditorDeleteState.Deleting
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable(initialName, stateSaver = TextFieldValue.Saver) {
        val text = initialName.orEmpty()
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    val focusOrder = remember { EditorFocusOrder() }
    val isEditing = initialName != null
    val mode = when {
        showDeleteConfirmation -> EditorActionMode.ConfirmDeletion
        isEditing -> EditorActionMode.Update
        else -> EditorActionMode.Create
    }
    val title = stringResource(
        if (isEditing) {
            Res.string.project_editor_edit_title
        } else {
            Res.string.project_editor_create_title
        },
    )

    AlertDialog(
        onDismissRequest = component::onClose,
        modifier = Modifier.testTag(PROJECT_EDITOR_TAG),
        title = { SelectableText(title) },
        text = {
            EditorFields(component, name, {
                name = it
            }, focusOrder, mode, onSave)
        },
        confirmButton = {
            EditorActions(
                mode = mode,
                name = name.text,
                focusOrder = focusOrder,
                isBusy,
                onSave,
                onClose = {
                    if (showDeleteConfirmation) {
                        showDeleteConfirmation = false
                    } else {
                        component.onClose()
                    }
                },
                onDelete = { showDeleteConfirmation = true },
            )
        },
    )
}
