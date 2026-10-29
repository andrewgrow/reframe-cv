package reframecv.ui.compose.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.providers.AiProviderConfiguration
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.InvalidProviderConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_key
import reframecv.shared.generated.resources.settings_key_hide
import reframecv.shared.generated.resources.settings_key_show
import reframecv.shared.generated.resources.settings_management_key
import reframecv.shared.generated.resources.settings_management_key_hide
import reframecv.shared.generated.resources.settings_management_key_show
import reframecv.ui.components.settings.ProviderEditorComponent
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.components.settings.SettingsError

@Composable
internal fun ProviderApiKeyField(state: ProviderEditorState, component: ProviderEditorComponent) {
    key(component) {
        ProviderSecretField(
            providerKey(state.connection),
            component::onKey,
            !state.busy,
            Modifier.fillMaxWidth().testTag("settings.key"),
            stringResource(Res.string.settings_key),
            stringResource(Res.string.settings_key_show) to
                stringResource(Res.string.settings_key_hide),
            state.error == SettingsError.Unauthorized ||
                (
                    state.error == SettingsError.Validation && providerKey(
                        state.connection,
                    ).isBlank()
                    ),
        )
    }
}

@Composable
internal fun ProviderManagementKeyField(
    state: ProviderEditorState,
    component: ProviderEditorComponent,
) {
    key(component) {
        ProviderSecretField(
            state.connection.openRouter.managementApiKey,
            component::onManagementKey,
            !state.busy,
            Modifier.fillMaxWidth().testTag("settings.management.key"),
            stringResource(Res.string.settings_management_key),
            stringResource(Res.string.settings_management_key_show) to
                stringResource(Res.string.settings_management_key_hide),
        )
    }
}

@Composable
private fun ProviderSecretField(
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier,
    label: String,
    descriptions: Pair<String, String>,
    isError: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, onChange, modifier,
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    if (visible) descriptions.second else descriptions.first,
                )
            }
        },
        label = { Text(label) },
        isError = isError,
    )
}

private fun providerKey(connection: ProviderConnection): String =
    when (val configuration = connection.configuration) {
        is AiProviderConfiguration -> configuration.apiKey
        is ApifyConfiguration -> configuration.apiToken
        is InvalidProviderConfiguration -> ""
    }
