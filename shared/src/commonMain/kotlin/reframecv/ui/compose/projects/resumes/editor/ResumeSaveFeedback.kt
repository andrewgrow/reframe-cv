package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_import_error
import reframecv.shared.generated.resources.resume_import_progress
import reframecv.shared.generated.resources.resume_import_success
import reframecv.shared.generated.resources.resume_save_error
import reframecv.shared.generated.resources.resume_save_success
import reframecv.shared.generated.resources.resume_validation_feedback
import reframecv.ui.components.projects.resumes.editor.ResumeImportState
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeSaveFeedback(
    state: ResumeSaveState,
    successfulSaves: Long,
    validationFailures: Long,
    importState: ResumeImportState = ResumeImportState.Idle,
) {
    var showSuccess by remember { mutableStateOf(false) }
    LaunchedEffect(state, successfulSaves, importState) {
        showSuccess = state == ResumeSaveState.Saved || importState == ResumeImportState.Imported
        if (showSuccess) {
            delay(NOTIFICATION_DURATION_MS.milliseconds)
            showSuccess = false
        }
    }
    val showValidationFailure = rememberValidationNotification(validationFailures)
    val success = !showValidationFailure && showSuccess &&
        (state == ResumeSaveState.Saved || importState == ResumeImportState.Imported)
    val message = FeedbackMessage(state, importState, showValidationFailure, success)
    val tokens = ReframeTheme.tokens
    val lineHeight = with(LocalDensity.current) { tokens.typography.bodyMedium.lineHeight.toDp() }
    Box(
        Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.medium)
            .padding(top = tokens.spacing.small).height(lineHeight).testTag("resume.feedback"),
        contentAlignment = Alignment.CenterStart,
    ) {
        key(message) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            ) {
                if (importState == ResumeImportState.Importing) {
                    CircularProgressIndicator(
                        Modifier.size(lineHeight).testTag("resume.import.progress"),
                        strokeWidth = tokens.outlineWidth,
                    )
                }
                SelectableText(
                    message,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    style = tokens.typography.bodyMedium,
                    maxLines = 1,
                    color = when {
                        success -> ReframeTheme.colorScheme.success
                        importState == ResumeImportState.Importing -> ReframeTheme.colorScheme.hint
                        else -> ReframeTheme.colorScheme.critical
                    },
                )
            }
        }
    }
}

@Composable
private fun FeedbackMessage(
    state: ResumeSaveState,
    importState: ResumeImportState,
    showValidationFailure: Boolean,
    success: Boolean,
): String = when {
    importState == ResumeImportState.Importing -> stringResource(Res.string.resume_import_progress)

    showValidationFailure -> stringResource(Res.string.resume_validation_feedback)

    importState == ResumeImportState.Failed -> stringResource(Res.string.resume_import_error)

    state == ResumeSaveState.Failed -> stringResource(Res.string.resume_save_error)

    success && importState == ResumeImportState.Imported ->
        stringResource(Res.string.resume_import_success)

    success -> stringResource(Res.string.resume_save_success)

    else -> ""
}

@Composable
private fun rememberValidationNotification(validationFailures: Long): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(validationFailures) {
        if (validationFailures > 0) {
            visible = true
            delay(NOTIFICATION_DURATION_MS.milliseconds)
            visible = false
        }
    }
    return visible
}

private const val NOTIFICATION_DURATION_MS = 3_000L
