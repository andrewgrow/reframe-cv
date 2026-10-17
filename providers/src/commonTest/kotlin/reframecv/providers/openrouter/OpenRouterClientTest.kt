package reframecv.providers.openrouter

import io.ktor.client.engine.mock.MockEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import reframecv.network.createNetworkClient
import reframecv.providers.AiDocument
import reframecv.providers.AiProviderError
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse

class OpenRouterClientTest {
    @Test
    fun unconfiguredProviderReturnsExplicitFailureWithoutSendingTheDocument() = runBlocking {
        var requests = 0
        val engine =
            MockEngine {
                requests++
                error("No request expected before choosing a provider")
            }
        val provider = OpenRouterClient(createNetworkClient(engine))
        try {
            val result = provider.execute(
                AiRequest(
                    "Recognize",
                    "{}",
                    listOf(
                        AiDocument(
                            "job.pdf",
                            "application/pdf",
                            "%PDF-1.7".encodeToByteArray(),
                        ),
                    ),
                ),
            )
            assertEquals(AiResponse.Failed(AiProviderError.NotConfigured), result)
            assertEquals(0, requests)
        } finally {
            provider.close()
        }
    }
}
