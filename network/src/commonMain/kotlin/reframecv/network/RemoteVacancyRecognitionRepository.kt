package reframecv.network

import io.ktor.client.HttpClient
import reframecv.recognition.RecognitionDocument
import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.VacancyRecognitionRepository

/** Reserves the provider integration boundary while recognition remains unconfigured. */
class RemoteVacancyRecognitionRepository(private val client: HttpClient) :
    VacancyRecognitionRepository {
    override suspend fun recognize(document: RecognitionDocument): RecognitionOutcome =
        RecognitionOutcome.Failed(RecognitionError.NotConfigured)

    override fun close() {
        client.close()
        client.engine.close()
    }
}
