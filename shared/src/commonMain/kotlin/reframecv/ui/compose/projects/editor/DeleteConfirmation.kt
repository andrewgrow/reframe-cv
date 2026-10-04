package reframecv.ui.compose.projects.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_full_delete
import reframecv.shared.generated.resources.project_delete_confirmation
import reframecv.shared.generated.resources.project_delete_error
import reframecv.shared.generated.resources.project_delete_warning
import reframecv.ui.components.projects.editor.EditorComponent
import reframecv.ui.components.projects.editor.EditorDeleteState
import reframecv.ui.components.projects.editor.isDeleteConfirmation
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun DeleteConfirmation(
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
