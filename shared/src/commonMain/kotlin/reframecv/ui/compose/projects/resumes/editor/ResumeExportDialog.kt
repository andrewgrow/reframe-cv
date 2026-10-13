package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_export_download
import reframecv.shared.generated.resources.resume_export_format
import reframecv.shared.generated.resources.resume_export_json
import reframecv.shared.generated.resources.resume_export_title
import reframecv.ui.compose.common.ResizableDialog
import reframecv.ui.theme.ReframeTheme

/** Presents export options before file generation is implemented. */
@Composable
internal fun ResumeExportDialog(onClose: () -> Unit) {
    ResizableDialog(
        onDismissRequest = onClose,
        modifier = Modifier.testTag("resume.export.dialog"),
        title = { Text(stringResource(Res.string.resume_export_title)) },
        text = { ExportFormatPicker() },
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
                TextButton(onClose) { Text(stringResource(Res.string.resume_cancel)) }
                TextButton(onClose) { Text(stringResource(Res.string.resume_export_download)) }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportFormatPicker() {
    var expanded by remember { mutableStateOf(false) }
    val json = stringResource(Res.string.resume_export_json)
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            value = json,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(Res.string.resume_export_format)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().testTag("resume.export.format")
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(
                text = { Text(json) },
                onClick = { expanded = false },
                modifier = Modifier.testTag("resume.export.json"),
            )
        }
    }
}
