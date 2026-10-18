package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.providers.AiModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConnection

/** Stores user-created API connections; supported implementations are defined in code. */
interface ProviderConnectionsRepository {
    fun observe(): Flow<List<ProviderConnection>>
    suspend fun save(connection: ProviderConnection): Long
    suspend fun activate(id: Long)
    suspend fun updateAccountInfo(
        id: Long,
        type: ApiProviderType,
        apiKey: String,
        info: ProviderAccountInfo,
    )
}

/** Retrieves provider account details through the configured external implementation. */
fun interface ProviderAccountRepository {
    suspend fun load(type: ApiProviderType, apiKey: String): ProviderAccountInfo
}

/** Loads models after checking the supplied credentials with the external service. */
fun interface ProviderModelsRepository {
    suspend fun load(type: ApiProviderType, apiKey: String): List<AiModel>
}

/** Reports setup failures without leaking HTTP requests or credentials into UI/logs. */
enum class ProviderSetupError { Unauthorized, Unavailable, InvalidResponse }

/** Carries only a safe error category across the provider boundary. */
class ProviderSetupException(val reason: ProviderSetupError) : Exception(reason.name)
