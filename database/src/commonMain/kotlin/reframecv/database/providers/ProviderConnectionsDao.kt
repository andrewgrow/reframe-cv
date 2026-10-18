package reframecv.database.providers

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** Persists connections and keeps a single active selection atomically. */
@Dao
abstract class ProviderConnectionsDao {
    @Query("SELECT * FROM provider_connections ORDER BY id ASC")
    abstract fun observe(): Flow<List<ProviderConnectionEntity>>

    @Query("SELECT * FROM provider_connections WHERE id = :id")
    abstract suspend fun get(id: Long): ProviderConnectionEntity?

    @Query("SELECT COUNT(*) FROM provider_connections")
    abstract suspend fun count(): Int

    @Insert
    abstract suspend fun insert(connection: ProviderConnectionEntity): Long

    @Update
    abstract suspend fun update(connection: ProviderConnectionEntity): Int

    @Query("UPDATE provider_connections SET active = 0 WHERE active = 1")
    abstract suspend fun clearActive()

    @Query("UPDATE provider_connections SET active = 1 WHERE id = :id")
    abstract suspend fun setActive(id: Long)

    @Query(
        "UPDATE provider_connections SET account_info = :info " +
            "WHERE id = :id AND type = :type AND api_key = :apiKey",
    )
    abstract suspend fun updateAccountInfo(id: Long, type: String, apiKey: String, info: String)

    @Transaction
    open suspend fun save(connection: ProviderConnectionEntity): Long {
        if (connection.id == 0L) return insert(connection.copy(active = count() == 0))
        val previous = checkNotNull(get(connection.id)) { "Connection does not exist" }
        check(
            update(
                connection.copy(
                    active = previous.active,
                    createdAt = previous.createdAt,
                    accountInfo = if (connection.apiKey == previous.apiKey &&
                        connection.type == previous.type
                    ) {
                        previous.accountInfo ?: connection.accountInfo
                    } else {
                        null
                    },
                ),
            ) == 1,
        )
        return connection.id
    }

    @Transaction
    open suspend fun activate(id: Long) {
        checkNotNull(get(id)) { "Connection does not exist" }
        clearActive()
        setActive(id)
    }
}
