package reframecv.ui.compose.projects.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.project_name
import reframecv.shared.generated.resources.project_save_error
import reframecv.ui.components.projects.editor.EditorComponent
import reframecv.ui.components.projects.editor.EditorDeleteState
import reframecv.ui.components.projects.editor.EditorSaveState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun EditorFields(
    component: EditorComponent,
    name: TextFieldValue,
    onNameChange: (TextFieldValue) -> Unit,
    focusOrder: EditorFocusOrder,
    mode: EditorActionMode,
    onSave: (String) -> Unit,
) {
    val saveState by component.saveState.subscribeAsState()
    val deleteState by component.deleteState.subscribeAsState()
    val isBusy = saveState == EditorSaveState.Saving || deleteState == EditorDeleteState.Deleting
    Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusOrder.name)
                .focusProperties {
                    if (mode == EditorActionMode.Update) {
                        next = if (name.text.isNotBlank()) focusOrder.update else focusOrder.cancel
                        previous = focusOrder.delete
                    }
                }
                .onFocusChanged {
                    if (it.isFocused) {
                        onNameChange(
                            name.copy(selection = TextRange(name.text.length)),
                        )
                    }
                },
            label = { Text(stringResource(Res.string.project_name)) },
            singleLine = true,
            enabled = !isBusy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (name.text.isNotBlank() && !isBusy && mode != EditorActionMode.ConfirmDeletion) {
                    onSave(name.text.trim())
                }
            }),
        )
        if (saveState == EditorSaveState.Failed) {
            SelectableText(
                stringResource(Res.string.project_save_error),
                color = ReframeTheme.colorScheme.critical,
            )
        }
        AnimatedVisibility(
            visible = mode == EditorActionMode.ConfirmDeletion,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            DeleteConfirmation(component, isBusy, deleteState)
        }
    }
    LaunchedEffect(Unit) { focusOrder.name.requestFocus() }
}
