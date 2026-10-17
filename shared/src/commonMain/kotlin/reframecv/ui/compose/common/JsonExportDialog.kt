package reframecv.ui.compose.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.export_download
import reframecv.shared.generated.resources.export_format
import reframecv.shared.generated.resources.export_json
import reframecv.ui.theme.ReframeTheme

/** Shows JSON export options, progress, and retryable file errors. */
@Composable
internal fun JsonExportDialog(
    title: String,
    failureMessage: String,
    state: JsonExportDialogState,
    onDownload: () -> Unit,
    onClose: () -> Unit,
    tagPrefix: String,
) {
    val busy = state == JsonExportDialogState.Exporting
    ResizableDialog(
        onDismissRequest = { if (!busy) onClose() },
        resizingEnabled = !busy,
        modifier = Modifier.testTag("$tagPrefix.dialog"),
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium)) {
                ExportFormatPicker(enabled = !busy, tagPrefix = tagPrefix)
                if (state == JsonExportDialogState.Failed) {
                    Text(
                        failureMessage,
                        color = ReframeTheme.colorScheme.critical,
                    )
                }
            }
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        Modifier.size(ReframeTheme.tokens.dimensions.iconSize)
                            .testTag("$tagPrefix.progress"),
                    )
                }
                TextButton(onClose, enabled = !busy) {
                    Text(stringResource(Res.string.action_cancel))
                }
                TextButton(onDownload, enabled = !busy) {
                    Text(stringResource(Res.string.export_download))
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportFormatPicker(enabled: Boolean, tagPrefix: String) {
    var expanded by remember { mutableStateOf(false) }
    val json = stringResource(Res.string.export_json)
    ExposedDropdownMenuBox(expanded && enabled, { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = json,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(Res.string.export_format)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().testTag("$tagPrefix.format")
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded && enabled, { expanded = false }) {
            DropdownMenuItem(
                text = { Text(json) },
                onClick = { expanded = false },
                modifier = Modifier.testTag("$tagPrefix.json"),
            )
        }
    }
}

/** Describes shared export-dialog progress without owning a feature's operations. */
internal enum class JsonExportDialogState { Idle, Exporting, Failed }
