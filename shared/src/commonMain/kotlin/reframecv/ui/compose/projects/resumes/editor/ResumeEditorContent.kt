package reframecv.ui.compose.projects.resumes.editor

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
import reframecv.shared.generated.resources.resume_create
import reframecv.shared.generated.resources.resume_edit
import reframecv.ui.components.projects.resumes.editor.ResumeDeleteState
import reframecv.ui.components.projects.resumes.editor.ResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.ResumeExportState
import reframecv.ui.components.projects.resumes.editor.ResumeImportState
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.compose.common.DocumentFooter
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.theme.ReframeTheme

@Composable
fun ResumeEditorContent(component: ResumeEditorComponent) {
    val exportVisible by component.exportVisible.subscribeAsState()
    val exportState by component.exportComponent.state.subscribeAsState()
    val importState by component.importComponent.state.subscribeAsState()
    val photoState by component.photoComponent.state.subscribeAsState()
    val photoVisible by component.photoComponent.visible.subscribeAsState()
    val draft by component.draft.subscribeAsState()
    val deleteState by component.deleteState.subscribeAsState()
    val saveState by component.saveState.subscribeAsState()
    val validationRequested by component.validationRequested.subscribeAsState()
    val busy = saveState == ResumeSaveState.Saving ||
        deleteState == ResumeDeleteState.Deleting || exportState == ResumeExportState.Exporting ||
        importState == ResumeImportState.Importing || photoState.loading
    val editing = component.initialResume != null
    val tokens = ReframeTheme.tokens
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            Modifier.testTag("resumes.editor").fillMaxSize()
                .background(MaterialTheme.colorScheme.background).safeContentPadding()
                .pageBackHandler(
                    component::onClose,
                    enabled =
                        !busy && !exportVisible && !photoVisible,
                ),
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(tokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
            ) {
                PageHeader(
                    stringResource(
                        if (editing) Res.string.resume_edit else Res.string.resume_create,
                    ),
                    component::onClose,
                    !busy,
                )
                ResumeEditorFields(
                    draft,
                    component::onDraftChange,
                    !busy,
                    validationRequested,
                    component.photoComponent,
                )
            }
            HorizontalDivider()
            ResumeEditorFooter(component)
        }
        ResumeEditorOverlays(component, photoVisible, deleteState, exportVisible)
    }
}

@Composable
private fun ResumeEditorOverlays(
    component: ResumeEditorComponent,
    photoVisible: Boolean,
    deleteState: ResumeDeleteState,
    exportVisible: Boolean,
) {
    if (photoVisible) ResumePhotoDialog(component.photoComponent)
    if (deleteState != ResumeDeleteState.Idle) ResumeDeleteConfirmation(component)
    if (exportVisible) {
        ResumeExportDialog(
            component.exportComponent,
            onClose = component::onCloseExport,
        )
    }
}

@Composable
private fun ResumeEditorFooter(component: ResumeEditorComponent) {
    val saveState by component.saveState.subscribeAsState()
    val successfulSaves by component.successfulSaves.subscribeAsState()
    val importState by component.importComponent.state.subscribeAsState()
    val validationFailures by component.validationFailures.subscribeAsState()
    DocumentFooter {
        Column {
            ResumeSaveFeedback(
                saveState,
                successfulSaves,
                importState = importState,
                validationFailures = validationFailures,
            )
            ResumeEditorActions(component, onExport = component::onExport)
        }
    }
}
