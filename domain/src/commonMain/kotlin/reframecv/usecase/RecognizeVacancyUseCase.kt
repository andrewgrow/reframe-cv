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
) {
    suspend operator fun invoke(source: RecognitionSource): RecognitionOutcome {
        val document = reader.read(source)
        currentCoroutineContext().ensureActive()
        val outcome = repository.recognize(document)
        currentCoroutineContext().ensureActive()
        return outcome
    }
}
