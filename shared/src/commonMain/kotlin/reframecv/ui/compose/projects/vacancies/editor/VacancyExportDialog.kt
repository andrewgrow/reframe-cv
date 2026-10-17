package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_export_error
import reframecv.shared.generated.resources.vacancy_export_title
import reframecv.ui.components.projects.vacancies.editor.VacancyExportComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyExportState
import reframecv.ui.compose.common.JsonExportDialog
import reframecv.ui.compose.common.JsonExportDialogState

@Composable
internal fun VacancyExportDialog(component: VacancyExportComponent, onClose: () -> Unit) {
    val state by component.state.subscribeAsState()
    JsonExportDialog(
        stringResource(Res.string.vacancy_export_title),
        stringResource(Res.string.vacancy_export_error),
        when (state) {
            VacancyExportState.Idle -> JsonExportDialogState.Idle
            VacancyExportState.Exporting -> JsonExportDialogState.Exporting
            VacancyExportState.Failed -> JsonExportDialogState.Failed
        },
        component::onDownload,
        onClose,
        "vacancy.export",
    )
}
