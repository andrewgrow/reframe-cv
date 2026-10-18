package reframecv.usecase

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.RecognitionSource
import reframecv.recognition.VacancyDocumentReader
import reframecv.recognition.VacancyRecognitionRepository

/** Prepares a source PDF and requests recognition without saving the vacancy. */
class RecognizeVacancyUseCase(
    private val reader: VacancyDocumentReader,
    private val repository: VacancyRecognitionRepository,
    private val providers: ProviderSettingsUseCases,
) {
    suspend operator fun invoke(
        source: RecognitionSource,
        connectionId: Long,
        instruction: String,
    ): RecognitionOutcome {
        require(instruction.isNotBlank())
        providers.requireUsableConnection(connectionId)
        val document = reader.read(source)
        currentCoroutineContext().ensureActive()
        val outcome = repository.recognize(
            document,
            providers.requireUsableConnection(connectionId),
            instruction,
        )
        currentCoroutineContext().ensureActive()
        providers.requireUsableConnection(connectionId)
        return outcome
    }
}
