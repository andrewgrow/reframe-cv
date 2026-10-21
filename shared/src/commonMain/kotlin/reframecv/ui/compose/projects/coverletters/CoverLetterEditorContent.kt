package reframecv.ui.compose.projects.coverletters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.letter_delete_error
import reframecv.shared.generated.resources.letter_save_error
import reframecv.shared.generated.resources.letter_save_success
import reframecv.shared.generated.resources.letter_title
import reframecv.shared.generated.resources.letter_validation
import reframecv.shared.generated.resources.resume_close
import reframecv.shared.generated.resources.resume_save
import reframecv.ui.components.projects.coverletters.CoverLetterEditorComponent
import reframecv.ui.components.projects.coverletters.LetterNotice
import reframecv.ui.compose.common.DocumentFooter
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.theme.ReframeTheme

@Composable
fun CoverLetterEditorContent(component: CoverLetterEditorComponent) {
    val state by component.state.subscribeAsState()
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .testTag("cover_letter.editor").pageBackHandler(
                component::onClose,
                enabled =
                    !state.busy && !state.confirming,
            ),
    ) {
        Box(Modifier.padding(ReframeTheme.tokens.spacing.medium)) {
            PageHeader(
                stringResource(Res.string.letter_title),
                component::onClose,
                enabled = !state.busy,
            )
        }
        CoverLetterFields(component, Modifier.weight(1f))
        HorizontalDivider()
        CoverLetterFooter(component)
    }
    CoverLetterDeleteDialog(component)
}

@Composable
private fun CoverLetterFooter(component: CoverLetterEditorComponent) {
    val state by component.state.subscribeAsState()
    val tokens = ReframeTheme.tokens
    DocumentFooter {
        Column(Modifier.fillMaxWidth().padding(tokens.spacing.medium)) {
            val notice = when (state.notice) {
                LetterNotice.None -> ""
                LetterNotice.Saved -> stringResource(Res.string.letter_save_success)
                LetterNotice.SaveFailed -> stringResource(Res.string.letter_save_error)
                LetterNotice.DeleteFailed -> stringResource(Res.string.letter_delete_error)
                LetterNotice.Validation -> stringResource(Res.string.letter_validation)
            }
            Text(
                notice,
                modifier = Modifier.testTag("cover_letter.notice"),
                color = if (state.notice ==
                    LetterNotice.Saved
                ) {
                    ReframeTheme.colorScheme.success
                } else {
                    ReframeTheme.colorScheme.critical
                },
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            ) {
                FillButton(
                    stringResource(Res.string.action_delete),
                    component::onDelete,
                    enabled =
                        !state.busy && !state.confirming,
                    fillColor = ReframeTheme.colorScheme.critical,
                    contentColor = ReframeTheme.colorScheme.critical,
                    outlineState = FillButton.OutlineState.Visible,
                    modifier = Modifier.testTag("cover_letter.delete"),
                )
                Spacer(Modifier.weight(1f))
                FillButton(
                    stringResource(Res.string.resume_save),
                    component::onSave,
                    enabled =
                        !state.busy && !state.confirming,
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
