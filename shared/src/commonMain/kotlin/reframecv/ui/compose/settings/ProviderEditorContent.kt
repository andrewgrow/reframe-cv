package reframecv.ui.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.InvalidProviderConfiguration
import reframecv.domain.models.providers.OpenAiConfiguration
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_cancel
import reframecv.shared.generated.resources.settings_close
import reframecv.shared.generated.resources.settings_edit
import reframecv.shared.generated.resources.settings_name
import reframecv.shared.generated.resources.settings_new
import reframecv.shared.generated.resources.settings_openai_hint
import reframecv.shared.generated.resources.settings_save
import reframecv.shared.generated.resources.settings_type
import reframecv.ui.components.settings.ProviderEditorComponent
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.components.settings.SettingsError
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.settings.runners.ProviderRunnersSection
import reframecv.ui.compose.settings.runners.RunnerEditorContent
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ProviderEditorContent(component: ProviderEditorComponent) {
    val slot by component.runners.subscribeAsState()
    val runners = slot.child?.instance
    if (runners != null) {
        val editor by runners.editorSlot.subscribeAsState()
        val child = editor.child?.instance
        if (child != null) {
            RunnerEditorContent(child)
            return
        }
    }
    ProviderConnectionContent(component)
}

@Composable
private fun ProviderConnectionContent(component: ProviderEditorComponent) {
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
                    !state.saving && !state.changingActive,
                )
                ProviderEditorFields(state, component)
            }
            HorizontalDivider()
            ProviderSaveFeedback(state)
            ProviderEditorActions(state, component)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderTypeField(state: ProviderEditorState, component: ProviderEditorComponent) {
    val enabled = !state.busy && state.connection.id == 0L
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded && enabled, { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = state.connection.type.name,
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
            ApiProviderType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        component.onType(type)
                        expanded = false
                    },
                    modifier = Modifier.testTag("settings.provider.${type.name.lowercase()}"),
                )
            }
        }
    }
}

@Composable
private fun ProviderEditorFields(state: ProviderEditorState, component: ProviderEditorComponent) {
    FastScrollColumn(
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        ProviderTypeField(state, component)
        ProviderNameField(state, component)
        ProviderStatusField(
            state.connection.active,
            !state.saving && !state.changingActive,
            component::onActiveChange,
        )
        when (state.connection.configuration) {
            is OpenRouterConfiguration -> {
                ProviderManagementSection(state, component)
                ProviderModelsSection(state, component)
            }

            is OpenAiConfiguration -> {
                ProviderModelsSection(state, component)
                Text(
                    stringResource(Res.string.settings_openai_hint),
                    color = ReframeTheme.colorScheme.hint,
                )
            }

            is ApifyConfiguration -> {
                ProviderApiKeyField(state, component)
                ProviderRunnersSection(component)
            }

            is InvalidProviderConfiguration -> Text(
                "Unsupported or damaged provider configuration.",
            )
        }
    }
}

@Composable
private fun ProviderNameField(state: ProviderEditorState, component: ProviderEditorComponent) {
    OutlinedTextField(
        state.connection.name,
        component::onName,
        Modifier.fillMaxWidth().testTag("settings.name"),
        enabled = !state.busy,
        singleLine = true,
        label = {
            Text(stringResource(Res.string.settings_name))
        },
    )
}

@Composable
private fun ProviderEditorActions(state: ProviderEditorState, component: ProviderEditorComponent) {
    val spacing = ReframeTheme.tokens.spacing
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
            enabled = !state.saving && !state.changingActive,
            outlineState = FillButton.OutlineState.Visible,
        )
        FillButton(
            stringResource(
                if (state.connection.id ==
                    0L
                ) {
                    Res.string.settings_cancel
                } else {
                    Res.string.settings_close
                },
            ),
            component::onBack,
            enabled = !state.saving && !state.changingActive,
        )
    }
}
