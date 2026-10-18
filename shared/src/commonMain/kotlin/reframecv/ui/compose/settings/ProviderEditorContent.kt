package reframecv.ui.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.providers.AiModel
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_cancel
import reframecv.shared.generated.resources.settings_choose_model
import reframecv.shared.generated.resources.settings_edit
import reframecv.shared.generated.resources.settings_key
import reframecv.shared.generated.resources.settings_key_hide
import reframecv.shared.generated.resources.settings_key_show
import reframecv.shared.generated.resources.settings_load_models
import reframecv.shared.generated.resources.settings_loading_models
import reframecv.shared.generated.resources.settings_model
import reframecv.shared.generated.resources.settings_model_hint
import reframecv.shared.generated.resources.settings_name
import reframecv.shared.generated.resources.settings_new
import reframecv.shared.generated.resources.settings_save
import reframecv.shared.generated.resources.settings_search
import reframecv.shared.generated.resources.settings_structured
import reframecv.shared.generated.resources.settings_type
import reframecv.ui.components.settings.ProviderEditorComponent
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.components.settings.SettingsError
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ProviderEditorContent(component: ProviderEditorComponent) {
    val state by component.state.subscribeAsState()
    val spacing = ReframeTheme.tokens.spacing
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            Modifier.fillMaxSize().background(
                MaterialTheme.colorScheme.background,
            ).testTag("settings.editor"),
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                PageHeader(
                    stringResource(
                        if (state.connection.id ==
                            0L
                        ) {
                            Res.string.settings_new
                        } else {
                            Res.string.settings_edit
                        },
                    ),
                    component::onBack,
                    !state.saving,
                )
                ProviderEditorFields(state, component)
            }
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(
                    spacing.medium,
                    androidx.compose.ui.Alignment.CenterHorizontally,
                ),
            ) {
                FillButton(
                    stringResource(Res.string.settings_save),
                    component::onSave,
                    Modifier.testTag("settings.save"),
                    enabled = !state.saving,
                    outlineState = FillButton.OutlineState.Visible,
                )
                FillButton(
                    stringResource(Res.string.settings_cancel),
                    component::onBack,
                    enabled = !state.saving,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderTypeField(enabled: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded && enabled, { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = "OpenRouter",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(Res.string.settings_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && enabled) },
            modifier = Modifier.fillMaxWidth().testTag("settings.provider.select")
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded && enabled, { expanded = false }) {
            DropdownMenuItem(
                text = { Text("OpenRouter") },
                onClick = { expanded = false },
                modifier = Modifier.testTag("settings.provider.openrouter"),
            )
        }
    }
}

@Composable
private fun ProviderModelField(state: ProviderEditorState, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var search by remember(state.models) { mutableStateOf("") }
    Text(stringResource(Res.string.settings_model), style = MaterialTheme.typography.titleSmall)
    if (state.models.isEmpty()) {
        if (state.connection.modelId.isNotBlank()) Text(state.connection.modelId)
        Text(stringResource(Res.string.settings_model_hint), color = ReframeTheme.colorScheme.hint)
    } else {
        val models = state.models.filter {
            it.name.contains(search, true) ||
                it.id.contains(search, true)
        }
        Column {
            OutlinedButton({
                expanded = true
            }, enabled = !state.busy, modifier = Modifier.testTag("settings.models.select")) {
                Text(
                    state.models.find { it.id == state.connection.modelId }?.name
                        ?: state.connection.modelId.takeIf(String::isNotBlank)
                        ?: stringResource(Res.string.settings_choose_model),
                )
            }
            DropdownMenu(expanded && !state.busy, {
                expanded = false
            }, Modifier.heightIn(max = ReframeTheme.tokens.dimensions.providerModelMenuMaxHeight)) {
                OutlinedTextField(
                    search,
                    {
                        search = it
                    },
                    Modifier.fillMaxWidth().testTag(
                        "settings.models.search",
                    ),
                    singleLine = true,
                    enabled = !state.busy,
                    label = {
                        Text(stringResource(Res.string.settings_search))
                    },
                )

                models.forEach { model ->
                    DropdownMenuItem(text = { ModelLabel(model) }, onClick = {
                        select(model.id)
                        expanded =
                            false
                    })
                }
            }
        }
    }
}

@Composable
private fun ModelLabel(model: AiModel) {
    Column {
        Text(model.name)
        Text(model.id, style = MaterialTheme.typography.bodySmall)
        if (model.structuredOutput) {
            Text(
                stringResource(Res.string.settings_structured),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ProviderEditorFields(state: ProviderEditorState, component: ProviderEditorComponent) {
    FastScrollColumn(
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        ProviderTypeField(!state.busy)
        ProviderCredentialFields(state, component)
        FillButton(
            stringResource(Res.string.settings_load_models),
            component::onLoadModels,
            Modifier.testTag("settings.models.load"),
            enabled =
                !state.busy && state.connection.apiKey.isNotBlank(),
            outlineState = FillButton.OutlineState.Visible,
        )
        if (state.loading) {
            CircularProgressIndicator(Modifier.testTag("settings.models.progress"))
            Text(stringResource(Res.string.settings_loading_models))
        }
        ProviderModelField(state, component::onModel)
        state.error?.let { SettingsErrorText(it) }
        ProviderAccountContent(state, component::onRefreshAccount)
    }
}

@Composable
private fun ProviderCredentialFields(
    state: ProviderEditorState,
    component: ProviderEditorComponent,
) {
    OutlinedTextField(
        state.connection.name,
        component::onName,
        Modifier.fillMaxWidth().testTag("settings.name"),
        enabled = !state.busy,
        singleLine = true,
        label = {
            Text(stringResource(Res.string.settings_name))
        },
        isError =
            state.error == SettingsError.Validation &&
                state.connection.name.isBlank(),
    )
    ProviderApiKeyField(state, component)
}

@Composable
private fun ProviderApiKeyField(state: ProviderEditorState, component: ProviderEditorComponent) {
    var visible by remember(component) { mutableStateOf(false) }
    OutlinedTextField(
        state.connection.apiKey,
        component::onKey,
        Modifier.fillMaxWidth().testTag("settings.key"),
        enabled = !state.busy,
        singleLine = true,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = !state.busy) {
                Icon(
                    imageVector = if (visible) {
                        Icons.Outlined.VisibilityOff
                    } else {
                        Icons.Outlined.Visibility
                    },
                    contentDescription = stringResource(
                        if (visible) Res.string.settings_key_hide else Res.string.settings_key_show,
                    ),
                )
            }
        },
        label = {
            Text(stringResource(Res.string.settings_key))
        },
        isError =
            state.error == SettingsError.Unauthorized ||
                (
                    state.error == SettingsError.Validation &&
                        state.connection.apiKey.isBlank()
                    ),
    )
}
