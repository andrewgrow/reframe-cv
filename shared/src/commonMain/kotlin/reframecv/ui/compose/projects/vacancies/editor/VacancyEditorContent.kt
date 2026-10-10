package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_editor_edit_title
import reframecv.shared.generated.resources.vacancy_editor_title
import reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState

internal const val VACANCY_EDITOR_TAG = "vacancies.editor"

private val draftSaver = listSaver<VacancyDraft, String>(
    save = { listOf(it.title, it.company, it.description, it.url, it.keywords) },
    restore = { VacancyDraft(it[0], it[1], it[2], it[3], it[4]) },
)

@Composable
fun VacancyEditorContent(component: VacancyEditorComponent) {
    val saveState by component.saveState.subscribeAsState()
    val deleteState by component.deleteState.subscribeAsState()
    val initial = component.initialVacancy
    var draft by rememberSaveable(initial?.id, stateSaver = draftSaver) {
        mutableStateOf(initial?.let(VacancyDraft::fromVacancy) ?: VacancyDraft(""))
    }
    val isBusy = saveState == VacancySaveState.Saving || deleteState == VacancyDeleteState.Deleting
    val confirming = deleteState != VacancyDeleteState.Idle
    val save = { if (!confirming && !isBusy) component.onSave(draft) }
    AlertDialog(
        modifier = Modifier.testTag(VACANCY_EDITOR_TAG),
        onDismissRequest = component::onClose,
        title = {
            SelectableText(
                stringResource(
                    if (initial == null) {
                        Res.string.vacancy_editor_title
                    } else {
                        Res.string.vacancy_editor_edit_title
                    },
                ),
            )
        },
        text = {
            VacancyEditorFields(draft, { draft = it }, saveState, save, !isBusy && !confirming)
        },
        confirmButton = {
            VacancyEditorActions(component, draft.title.isNotBlank(), isBusy, deleteState, save)
        },
    )
}
