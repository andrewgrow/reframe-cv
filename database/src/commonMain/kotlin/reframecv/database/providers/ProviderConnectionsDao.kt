package reframecv.database.providers

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.providers.OpenRouterCachedInfo
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConfigurationJson

/** Persists generic connections while protecting caches against stale credential responses. */
@Dao
abstract class ProviderConnectionsDao {
    @Query("SELECT * FROM provider_connections ORDER BY id ASC")
    abstract fun observe(): Flow<List<ProviderConnectionEntity>>

    @Query("SELECT * FROM provider_connections WHERE id = :id")
    abstract suspend fun get(id: Long): ProviderConnectionEntity?

    @Insert
    abstract suspend fun insert(connection: ProviderConnectionEntity): Long

    @Update
    abstract suspend fun update(connection: ProviderConnectionEntity): Int

    @Query("UPDATE provider_connections SET active = :active WHERE id = :id")
    abstract suspend fun updateActive(id: Long, active: Boolean): Int

    @Query("UPDATE provider_connections SET cached_info = :cache WHERE id = :id")
    abstract suspend fun updateCache(id: Long, cache: String?)

    @Transaction
    open suspend fun save(connection: ProviderConnectionEntity): Long {
        if (connection.id == 0L) return insert(connection)
        val previous = checkNotNull(get(connection.id)) { "Connection does not exist" }
        val current = connection.toDomainModel()
        val old = previous.toDomainModel()
        val config = current.configuration as? OpenRouterConfiguration
        val oldConfig = old.configuration as? OpenRouterConfiguration
        val cache = if (config != null && oldConfig != null) {
            val oldCache = old.openRouterCache
            val newCache = current.openRouterCache
            OpenRouterCachedInfo(
                if (config.apiKey == oldConfig.apiKey) {
                    oldCache.accountInfo ?: newCache.accountInfo
                } else {
                    null
                },
                if (config.managementApiKey == oldConfig.managementApiKey) {
                    oldCache.creditBalance ?: newCache.creditBalance
                } else {
                    null
                },
            )
        } else {
            null
        }
        check(
            update(
                connection.copy(
                    active = previous.active,
                    createdAt = previous.createdAt,
                    cachedInfo = ProviderConfigurationJson.encodeCache(cache),
                ),
            ) == 1,
        )
        return connection.id
    }

    @Transaction
    open suspend fun updateOpenRouterCache(
        id: Long,
        type: String,
        key: String,
        info: reframecv.domain.models.providers.ProviderAccountInfo? = null,
        balance: reframecv.domain.models.providers.ProviderCreditBalance? = null,
    ) {
        val connection = get(id)?.toDomainModel()
        val config = connection?.configuration as? OpenRouterConfiguration
        if (connection == null || config == null) return
        val enabled = connection.active && connection.type.name == type
        val expected = if (info != null) config.apiKey else config.managementApiKey
        if (!enabled || key != expected) return
        val cache = connection.openRouterCache
        updateCache(
            id,
            ProviderConfigurationJson.encodeCache(
                cache.copy(
                    accountInfo = info ?: cache.accountInfo,
                    creditBalance = balance ?: cache.creditBalance,
                ),
            ),
        )
    }

    @Transaction
    open suspend fun setActive(id: Long, active: Boolean) {
        check(updateActive(id, active) == 1) { "Connection does not exist" }
    }
}
