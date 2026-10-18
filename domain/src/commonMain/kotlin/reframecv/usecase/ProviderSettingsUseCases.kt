package reframecv.usecase

import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.repository.ProviderConnectionsRepository
import reframecv.repository.ProviderModelsRepository

/** Supplies connection management and model discovery to global settings. */
class ProviderSettingsUseCases(
    private val connections: ProviderConnectionsRepository,
    private val models: ProviderModelsRepository,
) {
    val supportedTypes: List<ApiProviderType> = ApiProviderType.entries
    fun observe() = connections.observe()
    suspend fun activate(id: Long) = connections.activate(id)
    suspend fun loadModels(type: ApiProviderType, apiKey: String) =
        models.load(type, apiKey.trim().also { require(it.isNotEmpty()) })

    suspend fun save(connection: ProviderConnection): Long {
        require(connection.name.isNotBlank())
        require(connection.apiKey.isNotBlank())
        require(connection.modelId.isNotBlank())
        return connections.save(
            connection.copy(
                name = connection.name.trim(),
                apiKey = connection.apiKey.trim(),
            ),
        )
    }
}
