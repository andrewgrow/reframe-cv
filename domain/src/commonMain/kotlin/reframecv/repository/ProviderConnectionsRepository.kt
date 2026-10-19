package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.providers.AiModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.ProviderCreditBalance

/** Stores user-created API connections; supported implementations are defined in code. */
interface ProviderConnectionsRepository {
    fun observe(): Flow<List<ProviderConnection>>
    suspend fun get(id: Long): ProviderConnection?
    suspend fun save(connection: ProviderConnection): Long
    suspend fun setActive(id: Long, active: Boolean)
    suspend fun updateCreditBalance(
        id: Long,
        type: ApiProviderType,
        managementApiKey: String,
        balance: ProviderCreditBalance,
    )
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
    suspend fun balance(type: ApiProviderType, managementApiKey: String): ProviderCreditBalance =
        throw ProviderSetupException(ProviderSetupError.Unavailable)
}

/** Loads models after checking the supplied credentials with the external service. */
fun interface ProviderModelsRepository {
    suspend fun load(type: ApiProviderType, apiKey: String): List<AiModel>
}

/** Reports setup failures without leaking HTTP requests or credentials into UI/logs. */
enum class ProviderSetupError { Unauthorized, Unavailable, InvalidResponse, Disabled }

/** Carries an error category and optional credential-redacted provider feedback. */
class ProviderSetupException(val reason: ProviderSetupError, val providerMessage: String? = null) :
    Exception(reason.name)
