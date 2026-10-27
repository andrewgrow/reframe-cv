package reframecv.ui.compose.settings.runners

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
import reframecv.domain.models.providers.ConnectionOperation
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.runner_operation
import reframecv.shared.generated.resources.runner_operation_ams
import reframecv.shared.generated.resources.runner_operation_linkedin
import reframecv.ui.components.settings.runners.RunnerDraft

@Composable
internal fun operationTitle(operation: ConnectionOperation): String = stringResource(
    when (operation) {
        ConnectionOperation.LinkedInJobSearch -> Res.string.runner_operation_linkedin
        ConnectionOperation.AmsJobSearch -> Res.string.runner_operation_ams
    },
)

@Composable
internal fun RunnerOperationField(
    draft: RunnerDraft,
    enabled: Boolean,
    onDraft: (RunnerDraft) -> Unit,
) {
    val operations = ConnectionOperation.entries
    RunnerSelectField(
        operationTitle(draft.operation),
        operations.map { operationTitle(it) },
        stringResource(Res.string.runner_operation),
        "runner.operation",
        enabled,
    ) { index ->
        onDraft(
            draft.copy(
                operation = operations[index],
                maxResults =
                    if (operations[index] == ConnectionOperation.AmsJobSearch) "50" else "100",
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RunnerSelectField(
    value: String,
    choices: List<String>,
    label: String,
    tag: String,
    enabled: Boolean,
    select: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded && enabled, { if (enabled) expanded = it }) {
        OutlinedTextField(
            value,
            {},
            Modifier.fillMaxWidth().testTag(tag)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && enabled) },
        )
        ExposedDropdownMenu(expanded && enabled, { expanded = false }) {
            choices.forEachIndexed { index, choice ->
                DropdownMenuItem(
                    text = { Text(choice) },
                    onClick = {
                        expanded = false
                        select(index)
                    },
                )
            }
        }
    }
}
