package reframecv.database.settings

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

/** Loads and saves device preferences in the local database. */
@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(settings: AppSettingsEntity)

    @Query(
        "UPDATE app_settings SET window_width = :width, window_height = :height, " +
            "window_maximized = :maximized WHERE id = 1",
    )
    suspend fun updateWindow(width: Float, height: Float, maximized: Boolean)

    @Transaction
    suspend fun saveWindow(width: Float, height: Float, maximized: Boolean) {
        insertIfMissing(AppSettingsEntity())
        updateWindow(width, height, maximized)
    }
}
