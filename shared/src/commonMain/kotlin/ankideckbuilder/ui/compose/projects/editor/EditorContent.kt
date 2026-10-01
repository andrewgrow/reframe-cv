package ankideckbuilder.ui.compose.projects.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import ankideckbuilder.shared.generated.resources.Res
import ankideckbuilder.shared.generated.resources.action_cancel
import ankideckbuilder.shared.generated.resources.action_create
import ankideckbuilder.shared.generated.resources.action_save
import ankideckbuilder.shared.generated.resources.project_editor_create_title
import ankideckbuilder.shared.generated.resources.project_editor_edit_title
import ankideckbuilder.shared.generated.resources.project_name
import ankideckbuilder.ui.components.projects.editor.EditorComponent
import org.jetbrains.compose.resources.stringResource

internal const val PROJECT_EDITOR_TAG = "projects.editor"

@Composable
fun EditorContent(
    component: EditorComponent,
    initialName: String? = null,
    onSave: (String) -> Unit = { component.onClose() },
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName.orEmpty()) }
    val focusRequester = remember { FocusRequester() }
    val isEditing = initialName != null
    val title = stringResource(
        if (isEditing) {
            Res.string.project_editor_edit_title
        } else {
            Res.string.project_editor_create_title
        },
    )
    val projectNameLabel = stringResource(Res.string.project_name)
    val confirmLabel = stringResource(
        if (isEditing) Res.string.action_save else Res.string.action_create,
    )
    val cancelLabel = stringResource(Res.string.action_cancel)

    AlertDialog(
        onDismissRequest = component::onClose,
        modifier = Modifier.testTag(PROJECT_EDITOR_TAG),
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                label = { Text(projectNameLabel) },
                singleLine = true,
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = component::onClose) {
                Text(cancelLabel)
            }
        },
    )
}
