package reframecv.providers.recognition

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import reframecv.domain.models.providers.ProviderConnection
import reframecv.providers.AiDocument
import reframecv.providers.AiProviderClient
import reframecv.providers.AiProviderError
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse
import reframecv.recognition.RecognitionDocument
import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.VacancyRecognitionRepository

/** Converts a generic provider response into vacancy recognition fields or typed failures. */
class RemoteVacancyRecognitionRepository(
    private val provider: AiProviderClient,
    private val responseSchema: String,
) : VacancyRecognitionRepository {
    override suspend fun recognize(
        document: RecognitionDocument,
        connection: ProviderConnection,
        instruction: String,
    ): RecognitionOutcome {
        val response = provider.execute(
            AiRequest(
                instruction = instruction,
                connection = connection,
                responseSchema = responseSchema,
                documents = listOf(
                    AiDocument(document.fileName, "application/pdf", document.bytes),
                ),
            ),
        )
        currentCoroutineContext().ensureActive()
        return when (response) {
            is AiResponse.Generated -> decode(response.content)

            is AiResponse.Failed -> RecognitionOutcome.Failed(
                when (response.error) {
                    AiProviderError.NotConfigured -> RecognitionError.NotConfigured
                    AiProviderError.Unavailable -> RecognitionError.ServiceUnavailable
                    AiProviderError.InvalidResponse -> RecognitionError.InvalidResponse
                },
                response.details,
            )
        }
    }

    private fun decode(content: String): RecognitionOutcome = try {
        VacancyRecognitionJson.decode(content).toOutcome()
    } catch (error: CancellationException) {
        throw error
    } catch (_: IllegalArgumentException) {
        RecognitionOutcome.Failed(RecognitionError.InvalidResponse)
    }
}
