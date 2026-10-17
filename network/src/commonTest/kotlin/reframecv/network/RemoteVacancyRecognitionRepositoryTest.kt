package reframecv.network

import io.ktor.client.engine.mock.MockEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import reframecv.recognition.RecognitionDocument
import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionOutcome

class RemoteVacancyRecognitionRepositoryTest {
    @Test
    fun unconfiguredProviderReturnsExplicitFailureWithoutSendingTheDocument() = runBlocking {
        var requests = 0
        val engine =
            MockEngine {
                requests++
                error("No request expected before choosing a provider")
            }
        val repository = RemoteVacancyRecognitionRepository(createNetworkClient(engine))
        try {
            val result = repository.recognize(
                RecognitionDocument("job.pdf", "%PDF-1.7".encodeToByteArray()),
            )
            assertEquals(RecognitionOutcome.Failed(RecognitionError.NotConfigured), result)
            assertEquals(0, requests)
        } finally {
            repository.close()
        }
    }
}
