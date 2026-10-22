package reframecv.ui.compose.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.ai_request_incomplete_provider
import reframecv.shared.generated.resources.ai_request_model
import reframecv.shared.generated.resources.ai_request_no_prompts
import reframecv.shared.generated.resources.ai_request_no_providers
import reframecv.shared.generated.resources.ai_request_prompt
import reframecv.shared.generated.resources.ai_request_provider
import reframecv.ui.components.common.ai.AiRequestDialogComponent
import reframecv.ui.components.common.ai.AiRequestDialogState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun AiRequestChoices(component: AiRequestDialogComponent, state: AiRequestDialogState) {
    val enabled = !state.loading && !state.running && !state.savingPrompt
    AiRequestChoice(
        stringResource(Res.string.ai_request_provider),
        state.connection?.let { it.name.ifBlank { it.type.name } }.orEmpty(),
        state.connections.map { it.id to it.name.ifBlank { it.type.name } },
        enabled,
        "ai.request.provider",
        component::onConnection,
    )
    if (!state.loading && state.connections.isEmpty()) {
        SelectableText(
            stringResource(Res.string.ai_request_no_providers),
            color = ReframeTheme.colorScheme.hint,
        )
    }
    state.connection?.let {
        SelectableText(
            stringResource(
                Res.string.ai_request_model,
                it.openRouter.modelId.ifBlank {
                    "—"
                },
            ),
        )
        if (it.openRouter.modelId.isBlank() || it.openRouter.apiKey.isBlank()) {
            SelectableText(
                stringResource(Res.string.ai_request_incomplete_provider),
                color = ReframeTheme.colorScheme.critical,
            )
        }
    }
    AiRequestChoice(
        stringResource(Res.string.ai_request_prompt),
        state.prompt?.let { it.title.ifBlank { "Prompt #${it.id}" } }.orEmpty(),
        state.prompts.map { it.id to it.title.ifBlank { "Prompt #${it.id}" } },
        enabled,
        "ai.request.prompt",
        component::onPrompt,
    )
    if (!state.loading && state.prompts.isEmpty()) {
        SelectableText(
            stringResource(Res.string.ai_request_no_prompts),
            color = ReframeTheme.colorScheme.hint,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiRequestChoice(
    label: String,
    value: String,
    choices: List<Pair<Long, String>>,
    enabled: Boolean,
    tag: String,
    select: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded && enabled, { if (enabled) expanded = it }) {
        OutlinedTextField(
            value,
            {},
            Modifier.fillMaxWidth().testTag(tag)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            label = { Text(label) },
            enabled = enabled && choices.isNotEmpty(),
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && enabled) },
        )
        ExposedDropdownMenu(expanded && enabled, { expanded = false }) {
            choices.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        select(id)
                        expanded = false
                    },
                    modifier = Modifier.testTag("$tag.$id"),
                )
            }
        }
    }
}
