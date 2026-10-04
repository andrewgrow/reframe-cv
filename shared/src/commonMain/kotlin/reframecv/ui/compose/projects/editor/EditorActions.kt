package reframecv.ui.compose.projects.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.action_create
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.action_update
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun EditorActions(
    mode: EditorActionMode,
    name: String,
    focusOrder: EditorFocusOrder,
    isBusy: Boolean,
    onSave: (String) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit,
) {
    val confirmLabel = stringResource(
        if (mode == EditorActionMode.Create) Res.string.action_create else Res.string.action_update,
    )
    val cancelLabel = stringResource(Res.string.action_cancel)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        EditorDeleteAction(mode, focusOrder, isBusy, onDelete)
        Spacer(Modifier.weight(1f))
        TextButton(
            modifier = editorActionFocus(
                focusOrder.cancel,
                focusOrder.delete,
                if (name.isNotBlank()) focusOrder.update else focusOrder.name,
                mode,
            ),
            onClick = onClose,
            enabled = !isBusy,
            contentPadding = ReframeTheme.tokens.textButtonContentPadding,
        ) {
            Text(cancelLabel)
        }
        AnimatedVisibility(
            visible = mode != EditorActionMode.ConfirmDeletion,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            TextButton(
                contentPadding = ReframeTheme.tokens.textButtonContentPadding,
                modifier = editorActionFocus(
                    focusOrder.update,
                    focusOrder.cancel,
                    focusOrder.name,
                    mode,
                ),
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank() && !isBusy && mode != EditorActionMode.ConfirmDeletion,
            ) {
                Text(confirmLabel)
            }
        }
    }
}

@Composable
private fun EditorDeleteAction(
    mode: EditorActionMode,
    focusOrder: EditorFocusOrder,
    isBusy: Boolean,
    onDelete: () -> Unit,
) {
    AnimatedVisibility(
        visible = mode == EditorActionMode.Update,
        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
    ) {
        TextButton(
            contentPadding = ReframeTheme.tokens.textButtonContentPadding,
            modifier = editorActionFocus(
                focusOrder.delete,
                focusOrder.name,
                focusOrder.cancel,
                mode,
            ),
            onClick = onDelete,
            enabled = !isBusy && mode == EditorActionMode.Update,
            colors = ButtonDefaults.textButtonColors(
                contentColor = ReframeTheme.colorScheme.critical,
            ),
        ) {
            Text(stringResource(Res.string.action_delete))
        }
    }
}

private fun editorActionFocus(
    requester: FocusRequester,
    next: FocusRequester,
    previous: FocusRequester,
    mode: EditorActionMode,
): Modifier = Modifier.focusRequester(requester).focusProperties {
    if (mode == EditorActionMode.Update) {
        this.next = next
        this.previous = previous
    }
}
