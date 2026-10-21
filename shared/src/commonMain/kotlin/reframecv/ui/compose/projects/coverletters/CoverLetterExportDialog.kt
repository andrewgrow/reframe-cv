package reframecv.ui.compose.projects.coverletters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.letter_export_error
import reframecv.shared.generated.resources.letter_export_title
import reframecv.ui.components.projects.coverletters.CoverLetterExportComponent
import reframecv.ui.components.projects.coverletters.CoverLetterExportState
import reframecv.ui.compose.common.JsonExportDialog
import reframecv.ui.compose.common.JsonExportDialogState

@Composable
internal fun CoverLetterExportDialog(component: CoverLetterExportComponent, onClose: () -> Unit) {
    val state by component.state.subscribeAsState()
    JsonExportDialog(
        stringResource(Res.string.letter_export_title),
        stringResource(Res.string.letter_export_error),
        when (state) {
            CoverLetterExportState.Idle -> JsonExportDialogState.Idle
            CoverLetterExportState.Exporting -> JsonExportDialogState.Exporting
            CoverLetterExportState.Failed -> JsonExportDialogState.Failed
        },
        component::onDownload,
        onClose,
        "cover_letter.export",
    )
}
