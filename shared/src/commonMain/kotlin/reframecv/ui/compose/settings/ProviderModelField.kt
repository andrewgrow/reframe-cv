package reframecv.ui.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_load_models
import reframecv.shared.generated.resources.settings_loading_models
import reframecv.shared.generated.resources.settings_model_hint
import reframecv.shared.generated.resources.settings_refresh_models
import reframecv.ui.components.settings.ProviderEditorComponent
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ProviderModelField(state: ProviderEditorState, component: ProviderEditorComponent) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        ProviderModelPicker(state, component::onModel, Modifier.weight(1f))
        FillButton(
            stringResource(
                if (state.models.isEmpty()) {
                    Res.string.settings_load_models
                } else {
                    Res.string.settings_refresh_models
                },
            ),
            component::onLoadModels,
            Modifier.testTag("settings.models.load"),
            enabled = state.connection.active && !state.busy &&
                state.connection.ai.apiKey.isNotBlank(),
            outlineState = FillButton.OutlineState.Visible,
        )
    }
    if (state.models.isEmpty()) {
        Text(
            stringResource(Res.string.settings_model_hint),
            color = ReframeTheme.colorScheme.hint,
        )
    }
    if (state.loading) {
        LinearProgressIndicator(Modifier.fillMaxWidth().testTag("settings.models.progress"))
        Text(stringResource(Res.string.settings_loading_models))
    }
}
