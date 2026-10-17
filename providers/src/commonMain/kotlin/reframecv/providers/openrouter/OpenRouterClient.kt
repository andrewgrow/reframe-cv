package reframecv.providers.openrouter

import io.ktor.client.HttpClient
import reframecv.providers.AiProviderClient
import reframecv.providers.AiProviderError
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse

/** Reserves OpenRouter-specific API handling until credentials and model selection are available. */
class OpenRouterClient(private val transport: HttpClient) : AiProviderClient {
    override suspend fun execute(request: AiRequest): AiResponse =
        AiResponse.Failed(AiProviderError.NotConfigured)

    override fun close() {
        transport.close()
        transport.engine.close()
    }
}
