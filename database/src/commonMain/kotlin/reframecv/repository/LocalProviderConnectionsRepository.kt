package reframecv.repository

import kotlinx.coroutines.flow.map
import reframecv.database.providers.ProviderConnectionsDao
import reframecv.database.providers.toEntity
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConfigurationJson
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.ProviderCreditBalance
import reframecv.shared.nowMillis

/** Keeps generic provider connections available across application launches. */
class LocalProviderConnectionsRepository(private val dao: ProviderConnectionsDao) :
    ProviderConnectionsRepository {
    override fun observe() = dao.observe().map { rows -> rows.map { it.toDomainModel() } }
    override suspend fun get(id: Long) = dao.get(id)?.toDomainModel()
    override suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active)
    override suspend fun updateCreditBalance(
        id: Long,
        type: ApiProviderType,
        managementApiKey: String,
        balance: ProviderCreditBalance,
    ) = dao.updateOpenRouterCache(id, type.name, managementApiKey, balance = balance)
    override suspend fun updateAccountInfo(
        id: Long,
        type: ApiProviderType,
        apiKey: String,
        info: ProviderAccountInfo,
    ) = dao.updateOpenRouterCache(id, type.name, apiKey, info = info)
    override suspend fun save(connection: ProviderConnection): Long {
        val now = nowMillis()
        return dao.save(connection.copy(createdAt = now, updatedAt = now).toEntity())
    }
}
