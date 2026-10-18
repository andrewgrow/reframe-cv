package reframecv.database.providers

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** Persists connections and their independent enabled states. */
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

    @Query(
        "UPDATE provider_connections SET account_info = :info " +
            "WHERE id = :id AND type = :type AND api_key = :apiKey AND active = 1",
    )
    abstract suspend fun updateAccountInfo(id: Long, type: String, apiKey: String, info: String)

    @Transaction
    open suspend fun save(connection: ProviderConnectionEntity): Long {
        if (connection.id == 0L) return insert(connection)
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
    open suspend fun setActive(id: Long, active: Boolean) {
        check(updateActive(id, active) == 1) { "Connection does not exist" }
    }
}
