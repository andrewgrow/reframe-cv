package reframecv.providers

import reframecv.domain.models.providers.ApiProviderType
import reframecv.repository.ProviderModelsRepository
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException

/** Routes model discovery to an installed provider implementation. */
class RemoteProviderModelsRepository(
    private val clients: Map<ApiProviderType, ProviderSetupClient>,
) : ProviderModelsRepository {
    override suspend fun load(type: ApiProviderType, apiKey: String) = (
        clients[type] ?: throw ProviderSetupException(
            ProviderSetupError.Unavailable,
        )
        ).loadModels(apiKey)
}
