package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_editor_edit_title
import reframecv.shared.generated.resources.vacancy_editor_title
import reframecv.shared.generated.resources.vacancy_title_required
import reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.theme.ReframeTheme

internal const val VACANCY_EDITOR_TAG = "vacancies.editor"

@Composable
fun VacancyEditorContent(component: VacancyEditorComponent) {
    val saveState by component.saveState.subscribeAsState()
    val deleteState by component.deleteState.subscribeAsState()
    val snapshot by component.snapshot.state.subscribeAsState()
    val draft by component.draft.subscribeAsState()
    val isBusy = saveState == VacancySaveState.Saving || deleteState == VacancyDeleteState.Deleting
    val confirming = deleteState != VacancyDeleteState.Idle
    val titleError = if (snapshot.selected != null && draft.title.isBlank()) {
        stringResource(Res.string.vacancy_title_required)
    } else {
        null
    }
    val save = { if (!confirming && !isBusy && !snapshot.selecting) component.onSave(draft) }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            Modifier.testTag(VACANCY_EDITOR_TAG).fillMaxSize()
                .background(MaterialTheme.colorScheme.background).safeContentPadding()
                .pageBackHandler(component::onClose, enabled = !isBusy),
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                PageHeader(
                    stringResource(
                        if (component.initialVacancy == null) {
                            Res.string.vacancy_editor_title
                        } else {
                            Res.string.vacancy_editor_edit_title
                        },
                    ),
                    component::onClose,
                    enabled = !isBusy,
                )
                VacancyEditorFields(
                    draft,
                    component::onDraftChange,
                    saveState,
                    save,
                    !isBusy && !confirming,
                    titleError,
                ) {
                    VacancySnapshotContent(component.snapshot, enabled = !isBusy && !confirming)
                }
            }
            HorizontalDivider()
            Column(Modifier.fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium)) {
                VacancyEditorActions(
                    component,
                    draft.title.isNotBlank() && !snapshot.selecting,
                    isBusy,
                    deleteState,
                    save,
                )
            }
        }
    }
}
