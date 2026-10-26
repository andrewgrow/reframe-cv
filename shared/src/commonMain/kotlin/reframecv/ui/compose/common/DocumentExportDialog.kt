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

/** Shows document formats, progress, and retryable file errors. */
@Composable
internal fun DocumentExportDialog(
    title: String,
    failureMessage: String,
    state: DocumentExportDialogState,
    onDownload: () -> Unit,
    onClose: () -> Unit,
    tagPrefix: String,
    formats: List<ExportFormatOption> = emptyList(),
    selectedFormat: String = "json",
    onFormatChange: (String) -> Unit = {},
    downloadEnabled: Boolean = true,
    additionalContent: @Composable () -> Unit = {},
) {
    val busy = state == DocumentExportDialogState.Exporting
    ResizableDialog(
        onDismissRequest = { if (!busy) onClose() },
        resizingEnabled = !busy,
        modifier = Modifier.testTag("$tagPrefix.dialog"),
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium)) {
                ExportFormatPicker(
                    enabled = !busy,
                    tagPrefix = tagPrefix,
                    formats = formats.ifEmpty {
                        listOf(ExportFormatOption("json", stringResource(Res.string.export_json)))
                    },
                    selectedFormat = selectedFormat,
                    onFormatChange = onFormatChange,
                )
                additionalContent()
                if (state == DocumentExportDialogState.Failed) {
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
                TextButton(
                    onClose,
                    enabled = !busy,
                    modifier = Modifier.testTag("$tagPrefix.cancel"),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
                TextButton(
                    onDownload,
                    enabled = !busy && downloadEnabled,
                    modifier = Modifier.testTag("$tagPrefix.download"),
                ) {
                    Text(stringResource(Res.string.export_download))
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportFormatPicker(
    enabled: Boolean,
    tagPrefix: String,
    formats: List<ExportFormatOption>,
    selectedFormat: String,
    onFormatChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = formats.first { it.id == selectedFormat }
    ExposedDropdownMenuBox(expanded && enabled, { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selected.label,
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
            formats.forEach { format ->
                DropdownMenuItem(
                    text = { Text(format.label) },
                    onClick = {
                        onFormatChange(format.id)
                        expanded = false
                    },
                    modifier = Modifier.testTag("$tagPrefix.${format.id}"),
                )
            }
        }
    }
}

/** Describes shared export-dialog progress without owning a feature's operations. */
internal enum class DocumentExportDialogState { Idle, Exporting, Failed }

/** Supplies a stable selection ID and localized label for an export format. */
internal data class ExportFormatOption(val id: String, val label: String)
