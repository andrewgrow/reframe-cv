package reframecv.ui.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.providers.AiModel
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_choose_model
import reframecv.shared.generated.resources.settings_model
import reframecv.shared.generated.resources.settings_models_no_matches
import reframecv.shared.generated.resources.settings_search
import reframecv.shared.generated.resources.settings_structured
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.theme.ReframeTheme

/** Lets users find a provider model by name or ID without scrolling through the entire catalog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProviderModelPicker(
    state: ProviderEditorState,
    select: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var search by remember(state.models) { mutableStateOf("") }
    val enabled = state.connection.active && !state.busy && state.models.isNotEmpty()
    val label =
        state.models.find { it.id == state.connection.modelId }?.name ?: state.connection.modelId
    ExposedDropdownMenuBox(expanded && enabled, {
        if (enabled) {
            if (it) search = ""
            expanded = it
        }
    }, modifier) {
        OutlinedTextField(
            value = if (expanded) search else label,
            onValueChange = {
                search = it
                expanded = true
            },
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(Res.string.settings_model)) },
            placeholder = {
                Text(
                    stringResource(
                        if (expanded) {
                            Res.string.settings_search
                        } else {
                            Res.string.settings_choose_model
                        },
                    ),
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && enabled) },
            modifier = Modifier.fillMaxWidth().testTag("settings.models.select")
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(expanded && enabled, { expanded = false }) {
            ProviderModelOptions(state.models, search) {
                select(it)
                expanded = false
            }
        }
    }
}

@Composable
private fun ProviderModelOptions(models: List<AiModel>, search: String, select: (String) -> Unit) {
    val scroll = rememberScrollState()
    val tokens = ReframeTheme.tokens
    val query = search.trim()
    val matches = remember(models, query) {
        models.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true)
        }
    }
    LaunchedEffect(query) { scroll.scrollTo(0) }
    FastScrollColumn(
        modifier = Modifier.height(tokens.dimensions.providerModelMenuMaxHeight)
            .padding(horizontal = tokens.spacing.small).testTag("settings.models.menu"),
        scrollState = scroll,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        if (matches.isEmpty()) {
            Text(
                stringResource(Res.string.settings_models_no_matches),
                Modifier.padding(tokens.spacing.small),
            )
        }
        matches.forEach { model ->
            DropdownMenuItem(
                text = { ModelLabel(model) },
                onClick = { select(model.id) },
                modifier = Modifier.padding(vertical = tokens.spacing.small)
                    .testTag("settings.models.option.${model.id}"),
            )
        }
    }
}

@Composable
private fun ModelLabel(model: AiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
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
