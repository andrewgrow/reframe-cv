package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.action_save
import reframecv.shared.generated.resources.vacancy_delete_confirm
import reframecv.shared.generated.resources.vacancy_delete_error
import reframecv.shared.generated.resources.vacancy_delete_question
import reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyEditorActions(
    component: VacancyEditorComponent,
    validTitle: Boolean,
    isBusy: Boolean,
    deleteState: VacancyDeleteState,
    onSave: () -> Unit,
) {
    val confirming = deleteState != VacancyDeleteState.Idle
    Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
        AnimatedVisibility(
            confirming,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) { VacancyDeleteQuestion(deleteState) }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
        ) {
            if (component.initialVacancy != null) {
                VacancyDeleteAction(component::onDelete, confirming, isBusy)
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = component::onClose,
                enabled = !isBusy,
                contentPadding = ReframeTheme.tokens.textButtonContentPadding,
            ) { Text(stringResource(Res.string.action_cancel)) }
            AnimatedVisibility(
                !confirming,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
            ) {
                TextButton(
                    onClick = onSave,
                    enabled = validTitle && !isBusy && !confirming,
                    contentPadding = ReframeTheme.tokens.textButtonContentPadding,
                ) { Text(stringResource(Res.string.action_save)) }
            }
        }
    }
}

@Composable
private fun VacancyDeleteQuestion(state: VacancyDeleteState) {
    Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
        SelectableText(
            stringResource(Res.string.vacancy_delete_question),
            color = ReframeTheme.colorScheme.critical,
        )
        if (state == VacancyDeleteState.Failed) {
            SelectableText(
                stringResource(Res.string.vacancy_delete_error),
                color = ReframeTheme.colorScheme.critical,
            )
        }
    }
}

@Composable
private fun VacancyDeleteAction(onDelete: () -> Unit, confirming: Boolean, isBusy: Boolean) {
    TextButton(
        onClick = onDelete,
        enabled = !isBusy,
        contentPadding = ReframeTheme.tokens.textButtonContentPadding,
        colors = ButtonDefaults.textButtonColors(contentColor = ReframeTheme.colorScheme.critical),
    ) {
        Text(
            stringResource(
                if (confirming) Res.string.vacancy_delete_confirm else Res.string.action_delete,
            ),
        )
    }
}
