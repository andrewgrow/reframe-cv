package reframecv.ui.compose.projects.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.action_create
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.action_full_delete
import reframecv.shared.generated.resources.action_update
import reframecv.shared.generated.resources.project_delete_confirmation
import reframecv.shared.generated.resources.project_delete_error
import reframecv.shared.generated.resources.project_delete_warning
import reframecv.shared.generated.resources.project_editor_create_title
import reframecv.shared.generated.resources.project_editor_edit_title
import reframecv.shared.generated.resources.project_name
import reframecv.shared.generated.resources.project_save_error
import reframecv.ui.components.projects.editor.EditorComponent
import reframecv.ui.components.projects.editor.EditorDeleteState
import reframecv.ui.components.projects.editor.EditorSaveState
import reframecv.ui.components.projects.editor.isDeleteConfirmation
import reframecv.ui.theme.ReframeTheme

internal const val PROJECT_EDITOR_TAG = "projects.editor"

private enum class EditorActionMode { Create, Update, ConfirmDeletion }

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

    AlertDialog(
        onDismissRequest = component::onClose,
        modifier = Modifier.testTag(PROJECT_EDITOR_TAG),
        title = { SelectableText(title) },
        text = {
            EditorFields(component, name, {
                name = it
            }, focusRequester, showDeleteConfirmation, onSave)
        },
        confirmButton = {
            EditorActions(
                mode = when {
                    showDeleteConfirmation -> EditorActionMode.ConfirmDeletion
                    isEditing -> EditorActionMode.Update
                    else -> EditorActionMode.Create
                },
                name,
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

@Composable
private fun EditorActions(
    mode: EditorActionMode,
    name: String,
    isBusy: Boolean,
    onSave: (String) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit,
) {
    val confirmLabel = stringResource(
        if (mode == EditorActionMode.Create) Res.string.action_create else Res.string.action_update,
    )
    val deleteLabel = stringResource(Res.string.action_delete)
    val cancelLabel = stringResource(Res.string.action_cancel)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        AnimatedVisibility(
            visible = mode == EditorActionMode.Update,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
        ) {
            TextButton(
                contentPadding = ReframeTheme.tokens.textButtonContentPadding,
                onClick = onDelete,
                enabled = !isBusy && mode == EditorActionMode.Update,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ReframeTheme.colorScheme.critical,
                ),
            ) {
                Text(deleteLabel)
            }
        }
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(
            visible = mode != EditorActionMode.ConfirmDeletion,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            TextButton(
                contentPadding = ReframeTheme.tokens.textButtonContentPadding,
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank() && !isBusy && mode != EditorActionMode.ConfirmDeletion,
            ) {
                Text(confirmLabel)
            }
        }
        TextButton(
            onClick = onClose,
            enabled = !isBusy,
            contentPadding = ReframeTheme.tokens.textButtonContentPadding,
        ) {
            Text(cancelLabel)
        }
    }
}

@Composable
private fun EditorFields(
    component: EditorComponent,
    name: String,
    onNameChange: (String) -> Unit,
    focusRequester: FocusRequester,
    showDeleteConfirmation: Boolean,
    onSave: (String) -> Unit,
) {
    val saveState by component.saveState.subscribeAsState()
    val deleteState by component.deleteState.subscribeAsState()
    val isBusy = saveState == EditorSaveState.Saving || deleteState == EditorDeleteState.Deleting
    Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            label = { Text(stringResource(Res.string.project_name)) },
            singleLine = true,
            enabled = !isBusy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (name.isNotBlank() && !isBusy && !showDeleteConfirmation) onSave(name.trim())
            }),
        )
        if (saveState == EditorSaveState.Failed) {
            SelectableText(
                stringResource(Res.string.project_save_error),
                color = ReframeTheme.colorScheme.critical,
            )
        }
        AnimatedVisibility(
            visible = showDeleteConfirmation,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            DeleteConfirmation(component, isBusy, deleteState)
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun DeleteConfirmation(
    component: EditorComponent,
    isBusy: Boolean,
    deleteState: EditorDeleteState,
) {
    var confirmation by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
        Spacer(Modifier)
        SelectableText(
            stringResource(Res.string.project_delete_warning),
            color = ReframeTheme.colorScheme.critical,
        )
        OutlinedTextField(
            value = confirmation,
            onValueChange = { confirmation = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            label = { Text(stringResource(Res.string.project_delete_confirmation)) },
            singleLine = true,
            enabled = !isBusy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (confirmation.isDeleteConfirmation() && !isBusy) component.onDelete(confirmation)
            }),
        )
        if (deleteState == EditorDeleteState.Failed) {
            SelectableText(
                stringResource(Res.string.project_delete_error),
                color = ReframeTheme.colorScheme.critical,
            )
        }
        Button(
            contentPadding = ReframeTheme.tokens.buttonContentPadding,
            onClick = { component.onDelete(confirmation) },
            enabled = confirmation.isDeleteConfirmation() && !isBusy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ReframeTheme.colorScheme.critical,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(stringResource(Res.string.action_full_delete))
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
