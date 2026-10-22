package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_delete_confirm
import reframecv.shared.generated.resources.runner_configuration_error
import reframecv.shared.generated.resources.runner_delete_error
import reframecv.shared.generated.resources.runner_delete_title
import reframecv.shared.generated.resources.runner_delete_warning
import reframecv.shared.generated.resources.runner_operation_linkedin
import reframecv.shared.generated.resources.runner_validation
import reframecv.shared.generated.resources.settings_cancel
import reframecv.shared.generated.resources.settings_close
import reframecv.shared.generated.resources.settings_error_save
import reframecv.shared.generated.resources.settings_save
import reframecv.shared.generated.resources.settings_saved
import reframecv.ui.components.common.confirmation.ConfirmationDialogAction
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogState
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.components.settings.runners.RunnerEditorComponent
import reframecv.ui.components.settings.runners.RunnerEditorError
import reframecv.ui.components.settings.runners.RunnerEditorState
import reframecv.ui.compose.common.ConfirmationDialog
import reframecv.ui.compose.common.DocumentListControls
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun RunnerActions(state: RunnerEditorState, component: RunnerEditorComponent) {
    val enabled = !state.busy && !state.confirming
    DocumentListControls(showDestructive = state.runner.id != 0L, destructive = {
        FillButton(
            stringResource(Res.string.action_delete),
            component::onDelete,
            Modifier.testTag("runner.delete"),
            enabled = enabled,
            outlineState = FillButton.OutlineState.Visible,
            fillColor = ReframeTheme.colorScheme.critical,
            contentColor = ReframeTheme.colorScheme.critical,
        )
    }) {
        FillButton(
            stringResource(Res.string.settings_save),
            component::onSave,
            Modifier.testTag("runner.save"),
            enabled = enabled,
            outlineState = FillButton.OutlineState.Visible,
        )
        FillButton(
            stringResource(
                if (state.runner.id ==
                    0L
                ) {
                    Res.string.settings_cancel
                } else {
                    Res.string.settings_close
                },
            ),
            component::onBack,
            Modifier.testTag("runner.close"),
            enabled = enabled,
        )
    }
}

@Composable
internal fun RunnerFeedback(state: RunnerEditorState) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(state.successfulSaves) {
        visible = state.successfulSaves > 0
        if (visible) {
            delay(RUNNER_NOTIFICATION_MILLIS)
            visible = false
        }
    }
    val message = when (state.error) {
        RunnerEditorError.Validation -> Res.string.runner_validation
        RunnerEditorError.Save -> Res.string.settings_error_save
        RunnerEditorError.Configuration -> Res.string.runner_configuration_error
        else -> if (visible && !state.busy) Res.string.settings_saved else null
    }
    val height = with(LocalDensity.current) {
        ReframeTheme.tokens.typography.bodyMedium.lineHeight.toDp() * RUNNER_FEEDBACK_LINES
    }
    Box(
        Modifier.fillMaxWidth().padding(horizontal = ReframeTheme.tokens.spacing.medium)
            .height(height).testTag("runner.feedback"),
    ) {
        Text(
            message?.let { stringResource(it) }.orEmpty(),
            maxLines = RUNNER_FEEDBACK_LINES,
            overflow = TextOverflow.Ellipsis,
            color =
                if (state.error ==
                    null
                ) {
                    ReframeTheme.colorScheme.success
                } else {
                    ReframeTheme.colorScheme.critical
                },
        )
    }
}

@Composable
internal fun RunnerDeleteConfirmation(state: RunnerEditorState, component: RunnerEditorComponent) {
    if (!state.confirming) return
    ConfirmationDialog(
        ConfirmationDialogContent(
            Res.string.runner_delete_title,
            Res.string.runner_delete_warning,
            Res.string.resume_delete_confirm,
            Res.string.resume_cancel,
            subject = state.runner.name.ifBlank {
                stringResource(Res.string.runner_operation_linkedin)
            },
            tone = ConfirmationDialogTone.Critical,
        ),
        ConfirmationDialogState(
            busy = state.deleting,
            error = if (state.error ==
                RunnerEditorError.Delete
            ) {
                Res.string.runner_delete_error
            } else {
                null
            },
        ),
        {
            if (it ==
                ConfirmationDialogAction.Confirm
            ) {
                component.onConfirmDelete()
            } else {
                component.onBack()
            }
        },
        modifier = Modifier.testTag("runner.delete.dialog"),
    )
}

private const val RUNNER_NOTIFICATION_MILLIS = 3_000L

private const val RUNNER_FEEDBACK_LINES = 2
