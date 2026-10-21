package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.common.ai.AiPromptPreference
import reframecv.ui.components.common.ai.AiRequestDialogSlot
import reframecv.ui.components.common.ai.AiRequestException
import reframecv.ui.components.common.ai.DefaultAiRequestDialogComponent
import reframecv.ui.context.AppComponentContext
import reframecv.usecase.CoverLetterGenerationException

/** Holds explicit resume selection before generating a letter from several linked resumes. */
data class LetterResumeSelection(val visible: Boolean = false, val resumeId: Long? = null)

/** Selects a linked resume and opens the shared AI workflow for letter generation. */
interface VacancyLetterGenerationComponent {
    val selection: Value<LetterResumeSelection>
    val dialog: Value<AiRequestDialogSlot>
    fun onGenerate()
    fun onResume(id: Long)
    fun onContinue()
    fun onCancel()
}

/** Creates independent letters while keeping provider errors in the AI dialog. */
class DefaultVacancyLetterGenerationComponent(
    private val context: AppComponentContext,
    private val vacancy: () -> Vacancy,
    private val resumes: () -> List<Resume>,
    private val created: (CoverLetter) -> Unit,
    private val canEdit: () -> Boolean,
    private val promptPreference: AiPromptPreference,
) : VacancyLetterGenerationComponent {
    override val selection = MutableValue(LetterResumeSelection())
    override val dialog = MutableValue(AiRequestDialogSlot())
    override fun onGenerate() {
        if (!canEdit() || dialog.value.component != null || selection.value.visible) return
        val choices = choices()
        if (choices.size ==
            1
        ) {
            open(choices.single())
        } else if (choices.isNotEmpty()) {
            selection.value =
                LetterResumeSelection(visible = true)
        }
    }
    override fun onResume(id: Long) {
        if (selection.value.visible &&
            choices().any { it.id == id }
        ) {
            selection.value = LetterResumeSelection(true, id)
        }
    }
    override fun onContinue() {
        if (!selection.value.visible) return
        choices().find { it.id == selection.value.resumeId }?.let(::open)
    }
    private fun choices(): List<Resume> {
        val target = vacancy()
        return resumes().filter {
            it.projectId == target.projectId &&
                target.resumes.any { link -> link.resumeId == it.id }
        }
    }
    private fun open(source: Resume) {
        val target = vacancy()
        selection.value = LetterResumeSelection()
        context.dependencies.eventHistory.record(
            "Cover letter generation opened(resumeId=${source.id}, vacancyId=${target.id})",
        )
        dialog.value = AiRequestDialogSlot(
            DefaultAiRequestDialogComponent(
                context,
                "${source.name} (resume.json) + ${target.name} (vacancy.json)",
                PromptComponent.CoverLetters,
                execute = { connection, instruction ->
                    try {
                        context.dependencies.generateCoverLetter(
                            source,
                            target,
                            connection.id,
                            instruction,
                        )
                    } catch (
                        error: CoverLetterGenerationException,
                    ) {
                        throw AiRequestException(error.message ?: "Generation failed.", error)
                    }
                },
                completed = {
                    created(it)
                    dialog.value = AiRequestDialogSlot()
                },
                dismissed = {
                    dialog.value = AiRequestDialogSlot()
                },
                promptPreference = promptPreference,
            ),
        )
    }
    override fun onCancel() {
        selection.value = LetterResumeSelection()
        dialog.value.component?.onClose()
    }
}
