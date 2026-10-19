package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import reframecv.assessment.ResumeAssessment
import reframecv.serialization.VacancyContentJson
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_assessment_stale
import reframecv.shared.generated.resources.resume_assessment_title
import reframecv.shared.generated.resources.resume_assessment_warning
import reframecv.shared.generated.resources.resume_close
import reframecv.shared.generated.resources.vacancy_resume_assess
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.compose.common.AiRequestDialogContent
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.ResizableDialog
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyAssessmentContent(component: VacancyEditorComponent) {
    val dialog by component.assessment.dialog.subscribeAsState()
    val selection by component.assessment.selection.subscribeAsState()
    val draft by component.draft.subscribeAsState()
    val resumes by component.resumes.state.subscribeAsState()
    dialog.component?.let {
        AiRequestDialogContent(
            it,
            stringResource(Res.string.vacancy_resume_assess),
            stringResource(Res.string.resume_assessment_warning),
        )
    }
    val result = draft.resumes.find { it.resumeId == selection.resumeId }?.assessment ?: return
    val resume = resumes.available.find { it.id == selection.resumeId }
    val current = draft.toVacancy(resume?.projectId ?: 0)
    val stale = resume == null || resume.updatedAt != result.resumeUpdatedAt ||
        VacancyContentJson.encode(current) != result.vacancyJson
    ResumeAssessmentDialog(result, stale, component.assessment::onCloseResult)
}

@Composable
internal fun ResumeAssessmentDialog(result: ResumeAssessment, stale: Boolean, close: () -> Unit) {
    ResizableDialog(
        onDismissRequest = close,
        modifier = Modifier.testTag("vacancy.assessment.result"),
        title = { Text(stringResource(Res.string.resume_assessment_title)) },
        text = {
            FastScrollColumn(
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                SelectableText(
                    "${result.result.score}%",
                    style = MaterialTheme.typography.headlineMedium,
                )
                SelectableText(result.result.summary)
                SelectableText("${result.providerName} · ${result.modelName}")
                SelectableText(Instant.fromEpochMilliseconds(result.assessedAt).toString())
                if (stale) {
                    SelectableText(
                        stringResource(Res.string.resume_assessment_stale),
                        color = ReframeTheme.colorScheme.critical,
                    )
                }
            }
        },
        actions = { FillButton(stringResource(Res.string.resume_close), close) },
    )
}
