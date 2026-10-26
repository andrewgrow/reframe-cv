package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.export_full_source_zip
import reframecv.shared.generated.resources.export_json
import reframecv.shared.generated.resources.export_pdf
import reframecv.shared.generated.resources.vacancy_export_error
import reframecv.shared.generated.resources.vacancy_export_title
import reframecv.ui.components.projects.vacancies.editor.VacancyExportComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyExportFormat
import reframecv.ui.components.projects.vacancies.editor.VacancyExportState
import reframecv.ui.compose.common.DocumentExportDialog
import reframecv.ui.compose.common.DocumentExportDialogState
import reframecv.ui.compose.common.ExportFormatOption

@Composable
internal fun VacancyExportDialog(component: VacancyExportComponent, onClose: () -> Unit) {
    val state by component.state.subscribeAsState()
    val format by component.format.subscribeAsState()
    val archive by component.archive.subscribeAsState()
    DocumentExportDialog(
        stringResource(Res.string.vacancy_export_title),
        stringResource(Res.string.vacancy_export_error),
        when (state) {
            VacancyExportState.Idle -> DocumentExportDialogState.Idle
            VacancyExportState.Exporting -> DocumentExportDialogState.Exporting
            VacancyExportState.Failed -> DocumentExportDialogState.Failed
        },
        component::onDownload,
        onClose,
        "vacancy.export",
        formats = listOf(
            ExportFormatOption("pdf", stringResource(Res.string.export_pdf)),
            ExportFormatOption("json", stringResource(Res.string.export_json)),
            ExportFormatOption("zip", stringResource(Res.string.export_full_source_zip)),
        ),
        selectedFormat = format.name.lowercase(),
        downloadEnabled = format != VacancyExportFormat.Zip || archive.ready,
        additionalContent = {
            if (format == VacancyExportFormat.Zip) {
                VacancyArchiveFields(archive, component, state == VacancyExportState.Exporting)
            }
        },
        onFormatChange = {
            component.onFormatChange(
                VacancyExportFormat.entries.first { format ->
                    format.name.equals(it, ignoreCase = true)
                },
            )
        },
    )
}
