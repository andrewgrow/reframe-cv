package reframecv.ui.compose.settings.prompts

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
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.prompts_error_save
import reframecv.shared.generated.resources.prompts_error_validation
import reframecv.shared.generated.resources.prompts_saved
import reframecv.ui.components.settings.prompts.PromptEditorError
import reframecv.ui.components.settings.prompts.PromptEditorState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun PromptSaveFeedback(state: PromptEditorState) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(state.successfulSaves) {
        visible = state.successfulSaves > 0
        if (visible) {
            delay(NOTIFICATION_MILLIS)
            visible = false
        }
    }
    val message = when (state.error) {
        PromptEditorError.Validation -> stringResource(Res.string.prompts_error_validation)
        PromptEditorError.Save -> stringResource(Res.string.prompts_error_save)
        null -> if (visible && !state.saving) stringResource(Res.string.prompts_saved) else ""
    }
    val tokens = ReframeTheme.tokens
    val height = with(LocalDensity.current) { tokens.typography.bodyMedium.lineHeight.toDp() }
    Box(
        Modifier.fillMaxWidth().padding(
            horizontal = tokens.spacing.medium,
        ).padding(top = tokens.spacing.small).height(height).testTag("prompts.feedback"),
    ) {
        Text(
            message,
            color = if (state.error ==
                null
            ) {
                ReframeTheme.colorScheme.success
            } else {
                ReframeTheme.colorScheme.critical
            },
        )
    }
}

private const val NOTIFICATION_MILLIS = 3_000L
