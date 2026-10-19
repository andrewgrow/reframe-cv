package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import reframecv.shared.generated.resources.vacancy_name_required
import reframecv.shared.generated.resources.vacancy_parse_pdf
import reframecv.shared.generated.resources.vacancy_parse_replace_warning
import reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyExportState
import reframecv.ui.components.projects.vacancies.editor.VacancyImportState
import reframecv.ui.components.projects.vacancies.editor.VacancyRecognitionState
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.compose.common.AiRequestDialogContent
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.theme.ReframeTheme

internal const val VACANCY_EDITOR_TAG = "vacancies.editor"

@Composable
fun VacancyEditorContent(component: VacancyEditorComponent) {
    val resumes by component.resumes.state.subscribeAsState()
    val aiDialog by component.recognition.dialog.subscribeAsState()
    val exportVisible by component.exportVisible.subscribeAsState()
    val exportState by component.exportComponent.state.subscribeAsState()
    val saveState by component.saveState.subscribeAsState()
    val deleteState by component.deleteState.subscribeAsState()
    val recognitionState by component.recognition.state.subscribeAsState()
    val importState by component.importComponent.state.subscribeAsState()
    val parsing = recognitionState == VacancyRecognitionState.Processing
    val isBusy = vacancyEditorBusy(
        saveState,
        deleteState,
        exportState,
        importState,
        recognitionState,
        aiDialog.component != null,
    )
    val confirming = deleteState != VacancyDeleteState.Idle
    val save = { component.onSave(component.draft.value) }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            Modifier.testTag(VACANCY_EDITOR_TAG).fillMaxSize()
                .background(MaterialTheme.colorScheme.background).safeContentPadding()
                .pageBackHandler(
                    component::onClose,
                    enabled =
                        (!isBusy || parsing) && !exportVisible && aiDialog.component == null &&
                            resumes.unlinkId == null,
                ),
        ) {
            VacancyEditorBody(
                component,
                saveState,
                save,
                enabled = !isBusy && !confirming && resumes.unlinkId == null,
                backEnabled = (!isBusy || parsing) && resumes.unlinkId == null,
                editing = component.initialVacancy != null,
            )
            HorizontalDivider()
            VacancyEditorFooter(
                component,
                isBusy || resumes.unlinkId != null,
                deleteState,
                save,
                editing =
                    component.initialVacancy != null,
            )
        }
        aiDialog.component?.let {
            AiRequestDialogContent(
                it,
                stringResource(Res.string.vacancy_parse_pdf),
                stringResource(Res.string.vacancy_parse_replace_warning),
            )
        }
        VacancyResumeUnlinkConfirmation(resumes, component.resumes)
        if (exportVisible) VacancyExportDialog(component.exportComponent, component::onCloseExport)
    }
}

@Composable
private fun vacancyNameError(hasAttachment: Boolean, name: String): String? = if (hasAttachment &&
    name.isBlank()
) {
    stringResource(Res.string.vacancy_name_required)
} else {
    null
}

@Composable
private fun vacancyEditorTitle(editing: Boolean) = stringResource(
    if (!editing) {
        Res.string.vacancy_editor_title
    } else {
        Res.string.vacancy_editor_edit_title
    },
)

@Composable
private fun VacancyEditorFooter(
    component: VacancyEditorComponent,
    isBusy: Boolean,
    deleteState: VacancyDeleteState,
    save: () -> Unit,
    editing: Boolean,
) {
    val importState by component.importComponent.state.subscribeAsState()
    val draft by component.draft.subscribeAsState()
    val snapshot by component.snapshot.state.subscribeAsState()
    Column(Modifier.fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium)) {
        VacancyImportFeedback(importState)
        VacancyEditorActions(
            component,
            draft.name.isNotBlank() && !snapshot.selecting,
            isBusy,
            deleteState,
            save,
            editing,
        )
    }
}

@Composable
private fun ColumnScope.VacancyEditorBody(
    component: VacancyEditorComponent,
    saveState: VacancySaveState,
    save: () -> Unit,
    enabled: Boolean,
    backEnabled: Boolean,
    editing: Boolean,
) {
    val resumes by component.resumes.state.subscribeAsState()
    val snapshot by component.snapshot.state.subscribeAsState()
    val draft by component.draft.subscribeAsState()
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
    ) {
        PageHeader(vacancyEditorTitle(editing), component::onClose, enabled = backEnabled)
        VacancyEditorFields(
            draft,
            component::onDraftChange,
            saveState,
            save,
            enabled,
            vacancyNameError(snapshot.selected != null, draft.name),
            component.details,
        ) {
            VacancySnapshotContent(
                component.snapshot,
                enabled = enabled,
                recognition = component.recognition,
                onRecognize = component::onRecognize,
            )
            Spacer(Modifier.height(ReframeTheme.tokens.spacing.large))
            VacancyResumesContent(draft.resumes, resumes, component.resumes, enabled)
        }
    }
}

private fun vacancyEditorBusy(
    save: VacancySaveState,
    delete: VacancyDeleteState,
    export: VacancyExportState,
    importing: VacancyImportState,
    recognition: VacancyRecognitionState,
    aiDialogOpen: Boolean,
) = save == VacancySaveState.Saving || delete == VacancyDeleteState.Deleting ||
    export == VacancyExportState.Exporting || importing == VacancyImportState.Importing ||
    recognition == VacancyRecognitionState.Processing || aiDialogOpen
