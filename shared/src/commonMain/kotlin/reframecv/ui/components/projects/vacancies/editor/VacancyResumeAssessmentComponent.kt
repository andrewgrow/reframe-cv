package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.assessment.ResumeAssessment
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.ui.components.common.ai.AiRequestDialogSlot
import reframecv.ui.components.common.ai.AiRequestException
import reframecv.ui.components.common.ai.DefaultAiRequestDialogComponent
import reframecv.ui.context.AppComponentContext
import reframecv.usecase.ResumeAssessmentException

/** Identifies an optional assessment result displayed by the vacancy page. */
data class ResumeAssessmentSelection(val resumeId: Long? = null)

/** Opens an assessment request and displays the last result for a linked resume. */
interface VacancyResumeAssessmentComponent {
    val dialog: Value<AiRequestDialogSlot>
    val selection: Value<ResumeAssessmentSelection>
    fun onAssess(id: Long)
    fun onView(id: Long)
    fun onCloseResult()
    fun onCancel()
}

/** Keeps assessment results in the vacancy draft and cancels work with its shared AI dialog. */
class DefaultVacancyResumeAssessmentComponent(
    private val context: AppComponentContext,
    private val vacancy: () -> Vacancy,
    private val resumes: () -> List<Resume>,
    private val changed: (List<VacancyResume>) -> Unit,
    private val canEdit: () -> Boolean,
) : VacancyResumeAssessmentComponent {
    override val dialog = MutableValue(AiRequestDialogSlot())
    override val selection = MutableValue(ResumeAssessmentSelection())

    override fun onAssess(id: Long) {
        if (!canEdit() || dialog.value.component != null || selection.value.resumeId != null) return
        val source = vacancy()
        val record = resumes().find {
            it.id == id && it.projectId == source.projectId &&
                source.resumes.any { link -> link.resumeId == id }
        } ?: return
        context.dependencies.eventHistory.record(
            "Resume assessment opened(resumeId=$id, vacancyId=${source.id})",
        )
        dialog.value = AiRequestDialogSlot(
            DefaultAiRequestDialogComponent(
                context,
                "${record.name} (resume.json) + ${source.name} (vacancy.json)",
                PromptComponent.Vacancies,
                execute = { connection, instruction ->
                    try {
                        context.dependencies.assessResume(
                            record,
                            source,
                            connection.id,
                            instruction,
                        )
                    } catch (error: ResumeAssessmentException) {
                        throw AiRequestException(error.message ?: "The assessment failed.", error)
                    }
                },
                completed = { assessment -> apply(id, assessment) },
                dismissed = { dialog.value = AiRequestDialogSlot() },
            ),
        )
    }

    private fun apply(id: Long, assessment: ResumeAssessment) {
        val current = vacancy().resumes
        if (current.any { it.resumeId == id } && resumes().any { it.id == id }) {
            changed(
                current.map {
                    if (it.resumeId ==
                        id
                    ) {
                        it.copy(atsScore = assessment.result.score, assessment = assessment)
                    } else {
                        it
                    }
                },
            )
            selection.value = ResumeAssessmentSelection(id)
        }
        dialog.value = AiRequestDialogSlot()
    }

    override fun onView(id: Long) {
        if (canEdit() && vacancy().resumes.any { it.resumeId == id && it.assessment != null }) {
            selection.value = ResumeAssessmentSelection(id)
        }
    }
    override fun onCloseResult() {
        selection.value = ResumeAssessmentSelection()
    }
    override fun onCancel() {
        dialog.value.component?.onClose()
    }
}
