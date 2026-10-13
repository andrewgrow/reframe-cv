package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_create
import reframecv.shared.generated.resources.resume_dates_error
import reframecv.shared.generated.resources.resume_edit
import reframecv.shared.generated.resources.resume_save
import reframecv.shared.generated.resources.resume_save_error
import reframecv.shared.generated.resources.resume_update
import reframecv.ui.components.projects.resumes.editor.ResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.theme.ReframeTheme

@Composable
fun ResumeEditorContent(component: ResumeEditorComponent) {
    val draft by component.draft.subscribeAsState()
    val saveState by component.saveState.subscribeAsState()
    val validationRequested by component.validationRequested.subscribeAsState()
    val busy = saveState == ResumeSaveState.Saving
    val editing = component.initialResume != null
    val tokens = ReframeTheme.tokens
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            Modifier.testTag("resumes.editor").fillMaxSize()
                .background(MaterialTheme.colorScheme.background).safeContentPadding()
                .pageBackHandler(component::onClose, enabled = !busy),
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
                if (saveState == ResumeSaveState.Failed) {
                    SelectableText(
                        stringResource(Res.string.resume_save_error),
                        color = ReframeTheme.colorScheme.critical,
                    )
                }
                if (validationRequested && draft.sections.any { !it.isValid }) {
                    SelectableText(
                        stringResource(Res.string.resume_dates_error),
                        color = ReframeTheme.colorScheme.critical,
                    )
                }
                ResumeEditorFields(draft, component::onDraftChange, !busy, validationRequested)
            }
            HorizontalDivider()
            ResumeEditorActions(component)
        }
    }
}
