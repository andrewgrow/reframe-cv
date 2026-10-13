package reframecv.ui.compose.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.ui.components.common.confirmation.ConfirmationDialogAction
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogState
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.theme.ReframeTheme

/** Displays a component's confirmation and reports choices without performing the operation. */
@Composable
internal fun ConfirmationDialog(
    content: ConfirmationDialogContent,
    state: ConfirmationDialogState,
    onAction: (ConfirmationDialogAction) -> Unit,
    onInputChange: (String) -> Unit = {},
    modifier: Modifier = Modifier.testTag("confirmation.dialog"),
    inputModifier: Modifier = Modifier.testTag("confirmation.input"),
) {
    val confirm = {
        if (state.confirmEnabled &&
            !state.busy
        ) {
            onAction(ConfirmationDialogAction.Confirm)
        }
    }
    ResizableDialog(
        onDismissRequest = { if (!state.busy) onAction(ConfirmationDialogAction.Back) },
        resizingEnabled = !state.busy,
        modifier = modifier,
        title = { SelectableText(stringResource(content.title)) },
        text = { ConfirmationFields(content, state, onInputChange, confirm, inputModifier) },
        actions = { ConfirmationActions(content, state, onAction, confirm) },
    )
}

@Composable
private fun ConfirmationFields(
    content: ConfirmationDialogContent,
    state: ConfirmationDialogState,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    inputModifier: Modifier,
) {
    val focus = remember { FocusRequester() }
    val color = if (content.tone == ConfirmationDialogTone.Critical) {
        ReframeTheme.colorScheme.critical
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
    ) {
        content.subject?.let { SelectableText(it) }
        SelectableText(stringResource(content.message), color = color)
        content.inputLabel?.let { label ->
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth().then(inputModifier).focusRequester(focus),
                label = { Text(stringResource(label)) },
                singleLine = true,
                enabled = !state.busy,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            )
        }
        state.error?.let { error ->
            SelectableText(stringResource(error), color = ReframeTheme.colorScheme.critical)
        }
    }
    LaunchedEffect(content.inputLabel) { if (content.inputLabel != null) focus.requestFocus() }
}

@Composable
private fun ConfirmationActions(
    content: ConfirmationDialogContent,
    state: ConfirmationDialogState,
    onAction: (ConfirmationDialogAction) -> Unit,
    onConfirm: () -> Unit,
) {
    val critical = content.tone == ConfirmationDialogTone.Critical
    FlowRow(horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
        FillButton(
            stringResource(content.confirmLabel),
            onConfirm,
            enabled = state.confirmEnabled && !state.busy,
            outlineState = FillButton.OutlineState.Visible,
            fillColor = if (critical) {
                ReframeTheme.colorScheme.critical
            } else {
                MaterialTheme.colorScheme.primary
            },
            contentColor = if (critical) {
                ReframeTheme.colorScheme.critical
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        FillButton(
            stringResource(content.cancelLabel),
            { if (!state.busy) onAction(ConfirmationDialogAction.Cancel) },
            enabled = !state.busy,
        )
    }
}
