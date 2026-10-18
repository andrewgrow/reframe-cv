package reframecv.providers

import reframecv.domain.models.providers.AiModel

/** Defines credential validation and model discovery independently of provider protocols. */
fun interface ProviderSetupClient {
    suspend fun loadModels(apiKey: String): List<AiModel>
}
