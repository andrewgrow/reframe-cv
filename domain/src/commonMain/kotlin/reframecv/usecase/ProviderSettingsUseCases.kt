package reframecv.usecase

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import reframecv.domain.models.providers.AiModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.ProviderCreditBalance
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

    suspend fun refreshCreditBalance(connection: ProviderConnection): ProviderCreditBalance {
        ensureEnabled(connection)
        val key = connection.managementApiKey.trim()
        require(key.isNotEmpty())
        return accounts.balance(connection.type, key).also { balance ->
            ensureEnabled(connection)
            if (connection.id != 0L) {
                connections.updateCreditBalance(connection.id, connection.type, key, balance)
            }
        }
    }

    suspend fun save(connection: ProviderConnection): Long {
        require(connection.apiKey.isNotBlank())
        return connections.save(
            connection.copy(
                name = connection.name.trim(),
                apiKey = connection.apiKey.trim(),
                managementApiKey = connection.managementApiKey.trim(),
                modelId = connection.modelId.trim(),
            ),
        )
    }

    suspend fun requireUsableConnection(id: Long): ProviderConnection {
        val connection = connections.get(id)
            ?: throw ProviderSetupException(ProviderSetupError.Disabled)
        ensureEnabled(connection)
        require(connection.apiKey.isNotBlank() && connection.modelId.isNotBlank())
        return connection
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
