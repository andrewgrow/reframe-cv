package reframecv.ui.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_management_section
import reframecv.shared.generated.resources.settings_models_section
import reframecv.ui.components.settings.ProviderEditorComponent
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.components.settings.SettingsError
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ProviderManagementSection(
    state: ProviderEditorState,
    component: ProviderEditorComponent,
) {
    ProviderSettingsSection(
        stringResource(Res.string.settings_management_section),
        "settings.management.section",
    ) {
        ProviderManagementKeyField(state, component)
        ProviderManagementContent(state, component::onRefreshBalance)
    }
}

@Composable
internal fun ProviderModelsSection(state: ProviderEditorState, component: ProviderEditorComponent) {
    ProviderSettingsSection(
        stringResource(Res.string.settings_models_section),
        "settings.models.section",
    ) {
        ProviderApiKeyField(state, component)
        ProviderModelField(state, component)
        state.error?.takeUnless {
            it == SettingsError.Save || it == SettingsError.Validation
        }?.let {
            SettingsErrorText(it, state.errorDetails)
        }
        if (state.connection.configuration is OpenRouterConfiguration) {
            ProviderAccountContent(state, component::onRefreshAccount)
        }
    }
}

@Composable
private fun ProviderSettingsSection(
    title: String,
    tag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = ReframeTheme.tokens.spacing
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = spacing.large).testTag(tag),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}
