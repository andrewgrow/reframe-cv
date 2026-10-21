package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.letter_generate
import reframecv.shared.generated.resources.letter_select_resume
import reframecv.shared.generated.resources.letter_unlink_question
import reframecv.shared.generated.resources.letter_unlink_title
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.vacancy_resume_unlink
import reframecv.ui.components.common.confirmation.ConfirmationDialogAction
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogState
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.compose.common.ConfirmationDialog
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.ResizableDialog
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyLetterDialogs(component: VacancyEditorComponent) {
    val letters by component.letters.state.subscribeAsState()
    letters.unlinkId?.let { id ->
        ConfirmationDialog(
            ConfirmationDialogContent(
                Res.string.letter_unlink_title,
                Res.string.letter_unlink_question,
                Res.string.vacancy_resume_unlink,
                Res.string.resume_cancel,
                subject = letters.available.find {
                    it.id ==
                        id
                }?.name,
                tone = ConfirmationDialogTone.Critical,
            ),
            ConfirmationDialogState(),
            {
                if (it ==
                    ConfirmationDialogAction.Confirm
                ) {
                    component.letters.onConfirmUnlink()
                } else {
                    component.letters.onCancelUnlink()
                }
            },
            modifier = Modifier.testTag("vacancy.letters.unlink.dialog"),
        )
    }
    LetterResumeSelectionDialog(component)
}

@Composable
private fun LetterResumeSelectionDialog(component: VacancyEditorComponent) {
    val selection by component.letterGeneration.selection.subscribeAsState()
    if (!selection.visible) return
    val resumes by component.resumes.state.subscribeAsState()
    val draft by component.draft.subscribeAsState()
    ResizableDialog(component.letterGeneration::onCancel, title = {
        Text(stringResource(Res.string.letter_select_resume))
    }, text = {
        FastScrollColumn(
            Modifier.heightIn(max = ReframeTheme.tokens.dimensions.providerModelMenuMaxHeight),
        ) {
            resumes.available.filter { resume ->
                draft.resumes.any { it.resumeId == resume.id }
            }.forEach { resume ->
                Row(
                    Modifier.fillMaxWidth().testTag(
                        "vacancy.letters.source.${resume.id}",
                    ).clickable {
                        component.letterGeneration.onResume(resume.id)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selection.resumeId == resume.id, {
                        component.letterGeneration.onResume(resume.id)
                    })
                    Text(resume.name)
                }
            }
        }
    }, actions = {
        FillButton(stringResource(Res.string.resume_cancel), component.letterGeneration::onCancel)
        FillButton(
            stringResource(Res.string.letter_generate),
            component.letterGeneration::onContinue,
            enabled =
                selection.resumeId != null,
            modifier = Modifier.testTag("vacancy.letters.continue"),
        )
    })
}
