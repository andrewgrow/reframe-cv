package reframecv.ui.compose.settings

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
import reframecv.shared.generated.resources.settings_error_save
import reframecv.shared.generated.resources.settings_error_validation
import reframecv.shared.generated.resources.settings_saved
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.components.settings.SettingsError
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ProviderSaveFeedback(state: ProviderEditorState) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(state.successfulSaves) {
        visible = state.successfulSaves > 0
        if (visible) {
            delay(NOTIFICATION_DURATION_MILLIS)
            visible = false
        }
    }
    val message = when (state.error) {
        SettingsError.Save -> stringResource(Res.string.settings_error_save)
        SettingsError.Validation -> stringResource(Res.string.settings_error_validation)
        else -> if (visible && !state.saving) stringResource(Res.string.settings_saved) else ""
    }
    val tokens = ReframeTheme.tokens
    val lineHeight = with(LocalDensity.current) { tokens.typography.bodyMedium.lineHeight.toDp() }
    Box(
        Modifier.fillMaxWidth().padding(
            horizontal = tokens.spacing.medium,
        ).padding(top = tokens.spacing.small).height(lineHeight).testTag("settings.feedback"),
    ) {
        Text(
            message,
            modifier = if (state.error == SettingsError.Save ||
                state.error == SettingsError.Validation
            ) {
                Modifier.testTag("settings.error")
            } else {
                Modifier
            },
            color = if (state.error ==
                SettingsError.Save ||
                state.error == SettingsError.Validation
            ) {
                ReframeTheme.colorScheme.critical
            } else {
                ReframeTheme.colorScheme.success
            },
            maxLines = 1,
        )
    }
}

private const val NOTIFICATION_DURATION_MILLIS = 3_000L
