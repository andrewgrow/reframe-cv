package reframecv.repository

import kotlinx.coroutines.flow.map
import reframecv.database.providers.ProviderConnectionEntity
import reframecv.database.providers.ProviderConnectionsDao
import reframecv.domain.models.providers.ProviderConnection
import reframecv.shared.nowMillis

/** Keeps provider connections available across application launches. */
class LocalProviderConnectionsRepository(private val dao: ProviderConnectionsDao) :
    ProviderConnectionsRepository {
    override fun observe() = dao.observe().map { rows -> rows.map { it.toDomainModel() } }
    override suspend fun activate(id: Long) = dao.activate(id)
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
            ),
        )
    }
}
