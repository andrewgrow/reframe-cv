package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.domain.models.prompts.PromptComponent
import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionFileException
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.RecognitionSource
import reframecv.recognition.RecognizedVacancy
import reframecv.ui.components.common.ai.AiRequestDialogComponent
import reframecv.ui.components.common.ai.AiRequestDialogSlot
import reframecv.ui.components.common.ai.AiRequestException
import reframecv.ui.components.common.ai.DefaultAiRequestDialogComponent
import reframecv.ui.context.AppComponentContext

/** Reports PDF processing without saving changes to the vacancy. */
sealed interface VacancyRecognitionState {
    data object Idle : VacancyRecognitionState
    data object Processing : VacancyRecognitionState
    data object Applied : VacancyRecognitionState
    data class Failed(val error: RecognitionError, val details: String? = null) :
        VacancyRecognitionState
}

/** Lets the editor start, cancel, or dismiss PDF recognition. */
interface VacancyRecognitionComponent {
    val dialog: Value<AiRequestDialogSlot>
    val state: Value<VacancyRecognitionState>
    fun onRecognize()
    fun onCancel()
    fun onReset()
}

/** Adapts the shared AI dialog to vacancy recognition and applies successful fields to the draft. */
class DefaultVacancyRecognitionComponent(
    private val context: AppComponentContext,
    private val source: () -> RecognitionSource?,
    private val recognized: (RecognizedVacancy) -> Unit,
    private val vacancyId: () -> Long?,
) : VacancyRecognitionComponent {
    override val state = MutableValue<VacancyRecognitionState>(VacancyRecognitionState.Idle)
    override val dialog = MutableValue(AiRequestDialogSlot())

    override fun onRecognize() {
        if (dialog.value.component != null) return
        val selected = source() ?: return
        val name = when (selected) {
            is RecognitionSource.Selected -> selected.file.fileName
            is RecognitionSource.Saved -> selected.attachment.fileName
        }
        context.dependencies.eventHistory.record(
            "Vacancy AI dialog opened(vacancyId=${vacancyId()})",
        )
        dialog.value = AiRequestDialogSlot(
            DefaultAiRequestDialogComponent(
                context,
                name,
                PromptComponent.Vacancies,
                execute = { connection, text -> recognize(selected, connection.id, text) },
                completed = {
                    recognized(it)
                    state.value = VacancyRecognitionState.Applied
                    dialog.value = AiRequestDialogSlot()
                },
                dismissed = {
                    state.value = VacancyRecognitionState.Idle
                    dialog.value = AiRequestDialogSlot()
                },
            ),
        )
    }

    private suspend fun recognize(
        source: RecognitionSource,
        connectionId: Long,
        text: String,
    ): RecognizedVacancy {
        state.value = VacancyRecognitionState.Processing
        val outcome = try {
            context.dependencies.recognizeVacancy(source, connectionId, text)
        } catch (error: RecognitionFileException) {
            state.value = VacancyRecognitionState.Failed(error.reason)
            throw AiRequestException(recognitionErrorMessage(error.reason), error)
        }
        return when (outcome) {
            is RecognitionOutcome.Recognized -> outcome.result

            is RecognitionOutcome.Failed -> {
                state.value = VacancyRecognitionState.Failed(outcome.error, outcome.details)
                throw AiRequestException(outcome.details ?: recognitionErrorMessage(outcome.error))
            }
        }
    }

    override fun onCancel() {
        dialog.value.component?.onClose()
        state.value = VacancyRecognitionState.Idle
    }

    override fun onReset() {
        if (dialog.value.component == null) state.value = VacancyRecognitionState.Idle
    }
}

private fun recognitionErrorMessage(error: RecognitionError): String = when (error) {
    RecognitionError.NotConfigured -> "Choose an active provider with an API key and model."
    RecognitionError.UnreadableFile -> "The source file could not be read."
    RecognitionError.UnsupportedFile -> "This file type is not supported."
    RecognitionError.InvalidPdf -> "The source is not a valid PDF."
    RecognitionError.ChangedFile -> "The source file has changed. Attach it again."
    RecognitionError.NotRecognized -> "No identifiable job posting was found in the source."
    RecognitionError.ServiceUnavailable -> "The provider could not complete the request."
    RecognitionError.InvalidResponse -> "The provider response does not match the vacancy schema."
}
