package reframecv.ui.compose.projects.resumes.editor

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
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_export_download
import reframecv.shared.generated.resources.resume_export_error
import reframecv.shared.generated.resources.resume_export_format
import reframecv.shared.generated.resources.resume_export_json
import reframecv.shared.generated.resources.resume_export_title
import reframecv.ui.components.projects.resumes.editor.ResumeExportComponent
import reframecv.ui.components.projects.resumes.editor.ResumeExportState
import reframecv.ui.compose.common.ResizableDialog
import reframecv.ui.theme.ReframeTheme

/** Shows JSON export options, progress, and retryable file errors. */
@Composable
internal fun ResumeExportDialog(component: ResumeExportComponent, onClose: () -> Unit) {
    val state by component.state.subscribeAsState()
    val busy = state == ResumeExportState.Exporting
    ResizableDialog(
        onDismissRequest = { if (!busy) onClose() },
        resizingEnabled = !busy,
        modifier = Modifier.testTag("resume.export.dialog"),
        title = { Text(stringResource(Res.string.resume_export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium)) {
                ExportFormatPicker(enabled = !busy)
                if (state == ResumeExportState.Failed) {
                    Text(
                        stringResource(Res.string.resume_export_error),
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
                            .testTag("resume.export.progress"),
                    )
                }
                TextButton(onClose, enabled = !busy) {
                    Text(stringResource(Res.string.resume_cancel))
                }
                TextButton(component::onDownload, enabled = !busy) {
                    Text(stringResource(Res.string.resume_export_download))
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportFormatPicker(enabled: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val json = stringResource(Res.string.resume_export_json)
    ExposedDropdownMenuBox(expanded && enabled, { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = json,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(Res.string.resume_export_format)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().testTag("resume.export.format")
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded && enabled, { expanded = false }) {
            DropdownMenuItem(
                text = { Text(json) },
                onClick = { expanded = false },
                modifier = Modifier.testTag("resume.export.json"),
            )
        }
    }
}
