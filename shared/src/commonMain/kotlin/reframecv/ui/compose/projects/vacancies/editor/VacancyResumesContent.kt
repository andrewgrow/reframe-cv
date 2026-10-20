package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.vacancy_resume
import reframecv.shared.generated.resources.vacancy_resume_assess
import reframecv.shared.generated.resources.vacancy_resume_ats
import reframecv.shared.generated.resources.vacancy_resume_missing
import reframecv.shared.generated.resources.vacancy_resume_unlink
import reframecv.shared.generated.resources.vacancy_resume_unlink_question
import reframecv.shared.generated.resources.vacancy_resume_unlink_title
import reframecv.shared.generated.resources.vacancy_resumes
import reframecv.shared.generated.resources.vacancy_resumes_error
import reframecv.shared.generated.resources.vacancy_resumes_retry
import reframecv.ui.components.common.confirmation.ConfirmationDialogAction
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogState
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.components.projects.vacancies.editor.VacancyResumeAssessmentComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyResumeImprovementComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyResumesComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyResumesState
import reframecv.ui.compose.common.ConfirmationDialog
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyResumesContent(
    links: List<VacancyResume>,
    state: VacancyResumesState,
    component: VacancyResumesComponent,
    enabled: Boolean,
    assessment: VacancyResumeAssessmentComponent? = null,
    improvement: VacancyResumeImprovementComponent? = null,
) {
    Column(
        Modifier.fillMaxWidth().testTag("vacancy.resumes"),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
    ) {
        Text(
            stringResource(Res.string.vacancy_resumes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        links.forEach { link ->
            key(link.resumeId) {
                val name = state.available.find { it.id == link.resumeId }?.name
                    ?: stringResource(Res.string.vacancy_resume_missing, link.resumeId)
                VacancyResumeRow(
                    link,
                    name,
                    enabled,
                    assessment,
                    improvement,
                    open = { component.onOpen(link.resumeId) },
                ) {
                    component.onUnlink(link.resumeId)
                }
            }
        }
        VacancyResumePicker(
            choices = state.available.filter { resume -> links.none { it.resumeId == resume.id } },
            enabled = enabled && !state.loading,
            select = component::onSelect,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.failed) {
            Text(
                stringResource(Res.string.vacancy_resumes_error),
                color = ReframeTheme.colorScheme.critical,
            )
            FillButton(
                stringResource(Res.string.vacancy_resumes_retry),
                component::onRetry,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun VacancyResumeRow(
    link: VacancyResume,
    name: String,
    enabled: Boolean,
    assessment: VacancyResumeAssessmentComponent?,
    improvement: VacancyResumeImprovementComponent?,
    open: () -> Unit,
    unlink: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().testTag("vacancy.resumes.row.${link.resumeId}"),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            label = { Text(stringResource(Res.string.vacancy_resume)) },
            modifier = Modifier.weight(1f),
        )
        ResumeOpenButton(link.resumeId, enabled, open)
        Text(
            stringResource(Res.string.vacancy_resume_ats, link.atsScore),
            color = if (link.assessment !=
                null
            ) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.widthIn(min = ReframeTheme.tokens.dimensions.vacancyAtsScoreWidth)
                .width(IntrinsicSize.Max)
                .testTag("vacancy.resumes.score.${link.resumeId}")
                .clickable(enabled = enabled && link.assessment != null, role = Role.Button) {
                    assessment?.onView(link.resumeId)
                },
        )
        ResumeImproveButton(link.resumeId, enabled) { improvement?.onImprove(link.resumeId) }
        FillButton(
            stringResource(Res.string.vacancy_resume_assess),
            { assessment?.onAssess(link.resumeId) },
            Modifier.testTag("vacancy.resumes.assess.${link.resumeId}"),
            enabled = enabled,
            outlineState = FillButton.OutlineState.Visible,
            fillColor = ReframeTheme.colorScheme.success,
            contentColor = ReframeTheme.colorScheme.success,
        )
        Spacer(Modifier.width(ReframeTheme.tokens.spacing.medium))
        FillButton(
            stringResource(Res.string.vacancy_resume_unlink),
            unlink,
            Modifier.testTag("vacancy.resumes.unlink.${link.resumeId}"),
            enabled = enabled,
            outlineState = FillButton.OutlineState.Visible,
            fillColor = ReframeTheme.colorScheme.critical,
            contentColor = ReframeTheme.colorScheme.critical,
        )
    }
}

@Composable
internal fun VacancyResumeUnlinkConfirmation(
    state: VacancyResumesState,
    component: VacancyResumesComponent,
) {
    val id = state.unlinkId ?: return
    ConfirmationDialog(
        content = ConfirmationDialogContent(
            title = Res.string.vacancy_resume_unlink_title,
            message = Res.string.vacancy_resume_unlink_question,
            confirmLabel = Res.string.vacancy_resume_unlink,
            cancelLabel = Res.string.resume_cancel,
            subject = state.available.find { it.id == id }?.name,
            tone = ConfirmationDialogTone.Critical,
        ),
        state = ConfirmationDialogState(),
        onAction = {
            if (it ==
                ConfirmationDialogAction.Confirm
            ) {
                component.onConfirmUnlink()
            } else {
                component.onCancelUnlink()
            }
        },
        modifier = Modifier.testTag("vacancy.resumes.unlink.dialog"),
    )
}
