package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_close
import reframecv.shared.generated.resources.resume_export
import reframecv.shared.generated.resources.resume_import
import reframecv.shared.generated.resources.resume_save
import reframecv.ui.components.projects.resumes.editor.ResumeDeleteState
import reframecv.ui.components.projects.resumes.editor.ResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.ResumeImportState
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeEditorActions(component: ResumeEditorComponent, onExport: () -> Unit) {
    val deleteState by component.deleteState.subscribeAsState()
    val saveState by component.saveState.subscribeAsState()
    val importState by component.importComponent.state.subscribeAsState()
    val busy = saveState == ResumeSaveState.Saving || deleteState != ResumeDeleteState.Idle ||
        importState == ResumeImportState.Importing
    val editing = component.initialResume != null
    val tokens = ReframeTheme.tokens
    FlowRow(
        Modifier.fillMaxWidth().padding(tokens.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        if (editing) {
            Box(Modifier.weight(1f)) {
                FillButton(
                    stringResource(Res.string.action_delete),
                    component::onRequestDelete,
                    enabled = !busy,
                    outlineState = FillButton.OutlineState.Visible,
                    fillColor = ReframeTheme.colorScheme.critical,
                    contentColor = ReframeTheme.colorScheme.critical,
                    filledContentColor = MaterialTheme.colorScheme.onError,
                )
            }
        }
        if (editing) {
            FillButton(
                stringResource(Res.string.resume_export),
                onExport,
                enabled = !busy,
                outlineState = FillButton.OutlineState.Visible,
            )
        }
        if (!editing) {
            FillButton(
                stringResource(Res.string.resume_import),
                component::onImport,
                enabled = !busy,
                outlineState = FillButton.OutlineState.Visible,
            )
        }
        FillButton(
            stringResource(Res.string.resume_save),
            component::onSave,
            enabled = !busy,
            outlineState = FillButton.OutlineState.Visible,
        )
        FillButton(
            stringResource(if (editing) Res.string.resume_close else Res.string.resume_cancel),
            component::onClose,
            enabled = !busy,
            outlineState = FillButton.OutlineState.Visible,
        )
    }
}
