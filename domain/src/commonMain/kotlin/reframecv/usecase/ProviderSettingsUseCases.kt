package reframecv.usecase

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import reframecv.domain.models.providers.AiModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConnection
import reframecv.repository.ProviderAccountRepository
import reframecv.repository.ProviderConnectionsRepository
import reframecv.repository.ProviderModelsRepository
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException

/** Supplies connection management and model discovery to global settings. */
class ProviderSettingsUseCases(
    private val connections: ProviderConnectionsRepository,
    private val models: ProviderModelsRepository,
    private val accounts: ProviderAccountRepository,
) {
    val supportedTypes: List<ApiProviderType> = ApiProviderType.entries
    fun observe() = connections.observe()
    suspend fun setActive(id: Long, active: Boolean) = connections.setActive(id, active)
    suspend fun loadModels(connection: ProviderConnection): List<AiModel> {
        ensureEnabled(connection)
        val result = models.load(
            connection.type,
            connection.apiKey.trim().also {
                require(it.isNotEmpty())
            },
        )
        ensureEnabled(connection)
        return result
    }

    suspend fun refreshAccountInfo(connection: ProviderConnection): ProviderAccountInfo {
        ensureEnabled(connection)
        return accounts.load(connection.type, connection.apiKey.trim()).also { info ->
            ensureEnabled(connection)
            if (connection.id != 0L) {
                connections.updateAccountInfo(
                    connection.id,
                    connection.type,
                    connection.apiKey.trim(),
                    info,
                )
            }
        }
    }

    suspend fun save(connection: ProviderConnection): Long {
        require(connection.apiKey.isNotBlank())
        return connections.save(
            connection.copy(
                name = connection.name.trim(),
                apiKey = connection.apiKey.trim(),
                modelId = connection.modelId.trim(),
            ),
        )
    }

    private suspend fun ensureEnabled(connection: ProviderConnection) {
        currentCoroutineContext().ensureActive()
        val storedActive = connection.id == 0L || connections.get(connection.id)?.active == true
        if (!connection.active ||
            !storedActive
        ) {
            throw ProviderSetupException(ProviderSetupError.Disabled)
        }
    }
}
