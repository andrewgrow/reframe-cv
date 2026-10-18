package reframecv.repository

import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import reframecv.database.providers.ProviderConnectionEntity
import reframecv.database.providers.ProviderConnectionsDao
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConnection
import reframecv.shared.nowMillis

/** Keeps provider connections available across application launches. */
class LocalProviderConnectionsRepository(private val dao: ProviderConnectionsDao) :
    ProviderConnectionsRepository {
    override fun observe() = dao.observe().map { rows -> rows.map { it.toDomainModel() } }
    override suspend fun activate(id: Long) = dao.activate(id)
    override suspend fun updateAccountInfo(
        id: Long,
        type: ApiProviderType,
        apiKey: String,
        info: ProviderAccountInfo,
    ) = dao.updateAccountInfo(id, type.name, apiKey, Json.encodeToString(info))
    override suspend fun save(connection: ProviderConnection): Long {
        val now = nowMillis()
        return dao.save(
            ProviderConnectionEntity(
                connection.id,
                connection.type.name,
                connection.name,
                connection.apiKey,
                connection.modelId,
                connection.active,
                now,
                now,
                connection.accountInfo?.let { Json.encodeToString(it) },
            ),
        )
    }
}
