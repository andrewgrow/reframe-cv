package reframecv.ui.compose.projects.coverletters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.export_json
import reframecv.shared.generated.resources.export_pdf
import reframecv.shared.generated.resources.letter_export_error
import reframecv.shared.generated.resources.letter_export_title
import reframecv.shared.generated.resources.letter_pdf_empty
import reframecv.ui.components.projects.coverletters.CoverLetterExportComponent
import reframecv.ui.components.projects.coverletters.CoverLetterExportFormat
import reframecv.ui.components.projects.coverletters.CoverLetterExportState
import reframecv.ui.compose.common.DocumentExportDialog
import reframecv.ui.compose.common.DocumentExportDialogState
import reframecv.ui.compose.common.ExportFormatOption

@Composable
internal fun CoverLetterExportDialog(component: CoverLetterExportComponent, onClose: () -> Unit) {
    val state by component.state.subscribeAsState()
    val format by component.format.subscribeAsState()
    DocumentExportDialog(
        stringResource(Res.string.letter_export_title),
        stringResource(
            if (state == CoverLetterExportState.EmptyText) {
                Res.string.letter_pdf_empty
            } else {
                Res.string.letter_export_error
            },
        ),
        when (state) {
            CoverLetterExportState.Idle -> DocumentExportDialogState.Idle

            CoverLetterExportState.Exporting -> DocumentExportDialogState.Exporting

            CoverLetterExportState.EmptyText, CoverLetterExportState.Failed ->
                DocumentExportDialogState.Failed
        },
        component::onDownload,
        onClose,
        "cover_letter.export",
        formats = listOf(
            ExportFormatOption("pdf", stringResource(Res.string.export_pdf)),
            ExportFormatOption("json", stringResource(Res.string.export_json)),
        ),
        selectedFormat = if (format == CoverLetterExportFormat.Pdf) "pdf" else "json",
        onFormatChange = {
            component.onFormatChange(
                if (it == "pdf") CoverLetterExportFormat.Pdf else CoverLetterExportFormat.Json,
            )
        },
    )
}
