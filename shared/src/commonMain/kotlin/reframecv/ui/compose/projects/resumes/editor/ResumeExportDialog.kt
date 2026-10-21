package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_export_error
import reframecv.shared.generated.resources.resume_export_title
import reframecv.ui.components.projects.resumes.editor.ResumeExportComponent
import reframecv.ui.components.projects.resumes.editor.ResumeExportState
import reframecv.ui.compose.common.DocumentExportDialog
import reframecv.ui.compose.common.DocumentExportDialogState

@Composable
internal fun ResumeExportDialog(component: ResumeExportComponent, onClose: () -> Unit) {
    val state by component.state.subscribeAsState()
    DocumentExportDialog(
        stringResource(Res.string.resume_export_title),
        stringResource(Res.string.resume_export_error),
        when (state) {
            ResumeExportState.Idle -> DocumentExportDialogState.Idle
            ResumeExportState.Exporting -> DocumentExportDialogState.Exporting
            ResumeExportState.Failed -> DocumentExportDialogState.Failed
        },
        component::onDownload,
        onClose,
        "resume.export",
    )
}
