package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import reframecv.shared.generated.resources.resume_dates_error
import reframecv.shared.generated.resources.resume_save_error
import reframecv.shared.generated.resources.resume_save_success
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeSaveFeedback(
    state: ResumeSaveState,
    successfulSaves: Long,
    invalidDates: Boolean,
) {
    var showSuccess by remember { mutableStateOf(false) }
    LaunchedEffect(state, successfulSaves) {
        showSuccess = state == ResumeSaveState.Saved
        if (showSuccess) {
            delay(SUCCESS_DURATION_MS.milliseconds)
            showSuccess = false
        }
    }
    val success = !invalidDates && showSuccess && state == ResumeSaveState.Saved
    val message = when {
        invalidDates -> stringResource(Res.string.resume_dates_error)
        state == ResumeSaveState.Failed -> stringResource(Res.string.resume_save_error)
        success -> stringResource(Res.string.resume_save_success)
        else -> ""
    }
    val tokens = ReframeTheme.tokens
    val lineHeight = with(LocalDensity.current) { tokens.typography.bodyMedium.lineHeight.toDp() }
    Box(
        Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.medium)
            .padding(top = tokens.spacing.small).height(lineHeight).testTag("resume.feedback"),
        contentAlignment = Alignment.CenterStart,
    ) {
        key(message) {
            SelectableText(
                message,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                style = tokens.typography.bodyMedium,
                maxLines = 1,
                color = if (success) {
                    ReframeTheme.colorScheme.success
                } else {
                    ReframeTheme.colorScheme.critical
                },
            )
        }
    }
}

private const val SUCCESS_DURATION_MS = 3_000L
