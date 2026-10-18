package reframecv.usecase

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.repository.ProviderAccountRepository
import reframecv.repository.ProviderConnectionsRepository
import reframecv.repository.ProviderModelsRepository

/** Supplies connection management and model discovery to global settings. */
class ProviderSettingsUseCases(
    private val connections: ProviderConnectionsRepository,
    private val models: ProviderModelsRepository,
    private val accounts: ProviderAccountRepository,
) {
    val supportedTypes: List<ApiProviderType> = ApiProviderType.entries
    fun observe() = connections.observe()
    suspend fun activate(id: Long) = connections.activate(id)
    suspend fun loadModels(type: ApiProviderType, apiKey: String) =
        models.load(type, apiKey.trim().also { require(it.isNotEmpty()) })

    suspend fun refreshAccountInfo(connection: ProviderConnection) =
        accounts.load(connection.type, connection.apiKey.trim()).also { info ->
            currentCoroutineContext().ensureActive()
            if (connection.id != 0L) {
                connections.updateAccountInfo(
                    connection.id,
                    connection.type,
                    connection.apiKey.trim(),
                    info,
                )
            }
        }

    suspend fun save(connection: ProviderConnection): Long {
        require(connection.name.isNotBlank())
        require(connection.apiKey.isNotBlank())
        return connections.save(
            connection.copy(
                name = connection.name.trim(),
                apiKey = connection.apiKey.trim(),
                modelId = connection.modelId.trim(),
            ),
        )
    }
}
