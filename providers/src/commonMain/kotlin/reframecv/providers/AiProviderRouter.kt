package reframecv.providers

import reframecv.domain.models.providers.ApiProviderType

/** Dispatches every AI operation to the implementation selected by its connection. */
class AiProviderRouter(private val clients: Map<ApiProviderType, AiProviderClient>) :
    AiProviderClient {
    override suspend fun execute(request: AiRequest): AiResponse {
        val connection = request.connection
        return if (connection.active && connection.usable) {
            clients[connection.type]?.execute(request)
                ?: AiResponse.Failed(AiProviderError.NotConfigured)
        } else {
            AiResponse.Failed(AiProviderError.NotConfigured)
        }
    }

    override fun close() {
        clients.values.distinct().forEach(AiProviderClient::close)
    }
}
