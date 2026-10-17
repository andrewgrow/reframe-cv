package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionFileException
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.RecognitionSource
import reframecv.recognition.VacancyRecognitionResult
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
    val state: Value<VacancyRecognitionState>
    fun onRecognize()
    fun onCancel()
    fun onReset()
}

/** Owns one cancellable recognition operation and discards late or disposed results. */
class DefaultVacancyRecognitionComponent(
    context: AppComponentContext,
    private val source: () -> RecognitionSource?,
    private val recognized: (VacancyRecognitionResult) -> Unit,
    private val vacancyId: () -> Long?,
) : VacancyRecognitionComponent {
    private val dependencies = context.dependencies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var generation = 0L
    private var closed = false
    private var job: Job? = null
    override val state = MutableValue<VacancyRecognitionState>(VacancyRecognitionState.Idle)

    init {
        context.lifecycle.doOnDestroy {
            closed = true
            onCancel()
            scope.cancel()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onRecognize() {
        if (closed || state.value == VacancyRecognitionState.Processing) return
        val selected = source() ?: return
        val request = ++generation
        state.value = VacancyRecognitionState.Processing
        dependencies.eventHistory.record("Vacancy Recognize PDF(vacancyId=${vacancyId()})")
        job = scope.launch {
            try {
                val document = dependencies.vacancyDocumentReader.read(selected)
                val outcome = dependencies.vacancyRecognitionRepository.recognize(document)
                currentCoroutineContext().ensureActive()
                if (request == generation && !closed) apply(outcome)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Recognize vacancy from PDF",
                    error,
                    mapOf("vacancyId" to vacancyId()),
                )
                if (request == generation && !closed) {
                    state.value = VacancyRecognitionState.Failed(
                        (error as? RecognitionFileException)?.reason
                            ?: RecognitionError.ServiceUnavailable,
                    )
                }
            }
        }
    }

    private fun apply(outcome: RecognitionOutcome) {
        state.value = when (outcome) {
            is RecognitionOutcome.Failed -> VacancyRecognitionState.Failed(
                outcome.error,
                outcome.details,
            )

            is RecognitionOutcome.Recognized -> when (outcome.result.statusCode) {
                VacancyRecognitionResult.SUCCESS -> {
                    recognized(outcome.result)
                    VacancyRecognitionState.Applied
                }

                VacancyRecognitionResult.NOT_RECOGNIZED ->
                    VacancyRecognitionState.Failed(
                        RecognitionError.NotRecognized,
                        outcome.result.statusDetails,
                    )

                else -> VacancyRecognitionState.Failed(
                    RecognitionError.ServiceUnavailable,
                    outcome.result.statusDetails,
                )
            }
        }
    }

    override fun onCancel() {
        generation++
        job?.cancel()
        state.value = VacancyRecognitionState.Idle
    }

    override fun onReset() {
        if (state.value !=
            VacancyRecognitionState.Processing
        ) {
            state.value = VacancyRecognitionState.Idle
        }
    }
}
