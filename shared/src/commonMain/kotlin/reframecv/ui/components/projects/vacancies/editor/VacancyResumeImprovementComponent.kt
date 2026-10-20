package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.common.ai.AiPromptPreference
import reframecv.ui.components.common.ai.AiRequestDialogSlot
import reframecv.ui.components.common.ai.AiRequestException
import reframecv.ui.components.common.ai.DefaultAiRequestDialogComponent
import reframecv.ui.context.AppComponentContext
import reframecv.usecase.ResumeImprovementException

/** Starts an independent AI adaptation of a resume linked to the current vacancy. */
interface VacancyResumeImprovementComponent {
    val dialog: Value<AiRequestDialogSlot>
    fun onImprove(id: Long)
    fun onCancel()
}

/** Returns a saved adaptation to the vacancy while failures remain in the shared AI dialog. */
class DefaultVacancyResumeImprovementComponent(
    private val context: AppComponentContext,
    private val vacancy: () -> Vacancy,
    private val resumes: () -> List<Resume>,
    private val improved: (Resume) -> Unit,
    private val canEdit: () -> Boolean,
    private val promptPreference: AiPromptPreference? = null,
) : VacancyResumeImprovementComponent {
    override val dialog = MutableValue(AiRequestDialogSlot())

    override fun onImprove(id: Long) {
        if (!canEdit() || dialog.value.component != null) return
        val target = vacancy()
        val source = resumes().find {
            it.id == id && it.projectId == target.projectId &&
                target.resumes.any { link -> link.resumeId == id }
        } ?: return
        context.dependencies.eventHistory.record(
            "Resume improvement opened(resumeId=$id, vacancyId=${target.id})",
        )
        dialog.value = AiRequestDialogSlot(
            DefaultAiRequestDialogComponent(
                context,
                "${source.name} (resume.json) + ${target.name} (vacancy.json)",
                PromptComponent.Vacancies,
                execute = { connection, instruction ->
                    try {
                        context.dependencies.improveResume(
                            source,
                            target,
                            connection.id,
                            instruction,
                        )
                    } catch (error: ResumeImprovementException) {
                        throw AiRequestException(error.message ?: "The improvement failed.", error)
                    }
                },
                completed = { record ->
                    improved(record)
                    dialog.value = AiRequestDialogSlot()
                },
                dismissed = { dialog.value = AiRequestDialogSlot() },
                promptPreference = promptPreference,
            ),
        )
    }

    override fun onCancel() {
        dialog.value.component?.onClose()
    }
}
