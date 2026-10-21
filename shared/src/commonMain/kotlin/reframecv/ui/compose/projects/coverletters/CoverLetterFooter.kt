package reframecv.ui.compose.projects.coverletters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.action_export
import reframecv.shared.generated.resources.letter_delete_error
import reframecv.shared.generated.resources.letter_save_error
import reframecv.shared.generated.resources.letter_save_success
import reframecv.shared.generated.resources.letter_validation
import reframecv.shared.generated.resources.resume_close
import reframecv.shared.generated.resources.resume_import
import reframecv.shared.generated.resources.resume_save
import reframecv.ui.components.projects.coverletters.CoverLetterEditorComponent
import reframecv.ui.components.projects.coverletters.LetterNotice
import reframecv.ui.compose.common.DocumentFooter
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun CoverLetterFooter(component: CoverLetterEditorComponent) {
    val state by component.state.subscribeAsState()
    val tokens = ReframeTheme.tokens
    val enabled = !state.busy && !state.confirming && !state.exportVisible
    DocumentFooter {
        Column(Modifier.fillMaxWidth().padding(tokens.spacing.medium)) {
            CoverLetterNotice(state.notice)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            ) {
                if (state.record.id > 0) {
                    FillButton(
                        stringResource(Res.string.action_delete),
                        component::onDelete,
                        enabled = enabled,
                        fillColor = ReframeTheme.colorScheme.critical,
                        contentColor = ReframeTheme.colorScheme.critical,
                        outlineState = FillButton.OutlineState.Visible,
                        modifier = Modifier.testTag("cover_letter.delete"),
                    )
                }
                Spacer(Modifier.weight(1f))
                FillButton(
                    stringResource(
                        if (state.record.id ==
                            0L
                        ) {
                            Res.string.resume_import
                        } else {
                            Res.string.action_export
                        },
                    ),
                    { if (state.record.id == 0L) component.onImport() else component.onExport() },
                    enabled = enabled,
                    modifier = Modifier.testTag("cover_letter.exchange"),
                )
                FillButton(
                    stringResource(Res.string.resume_save),
                    component::onSave,
                    enabled = enabled,
                    modifier = Modifier.testTag("cover_letter.save"),
                )
                FillButton(
                    stringResource(Res.string.resume_close),
                    component::onClose,
                    enabled = !state.busy,
                    modifier = Modifier.testTag("cover_letter.close"),
                )
            }
        }
    }
}

@Composable
private fun CoverLetterNotice(value: LetterNotice) {
    val notice = when (value) {
        LetterNotice.None -> ""
        LetterNotice.Saved -> stringResource(Res.string.letter_save_success)
        LetterNotice.SaveFailed -> stringResource(Res.string.letter_save_error)
        LetterNotice.DeleteFailed -> stringResource(Res.string.letter_delete_error)
        LetterNotice.Validation -> stringResource(Res.string.letter_validation)
    }
    Text(
        notice,
        modifier = Modifier.testTag("cover_letter.notice"),
        color = if (value ==
            LetterNotice.Saved
        ) {
            ReframeTheme.colorScheme.success
        } else {
            ReframeTheme.colorScheme.critical
        },
    )
}
