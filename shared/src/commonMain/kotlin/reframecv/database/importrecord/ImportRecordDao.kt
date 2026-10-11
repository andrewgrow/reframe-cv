package reframecv.database.importrecord

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Stores original import snapshots and removes snapshots no longer used by vacancies. */
@Dao
interface ImportRecordDao {
    @Query("SELECT file_path FROM import_records WHERE file_path IS NOT NULL")
    suspend fun filePaths(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM import_records WHERE file_path = :path)")
    suspend fun referencesFile(path: String): Boolean

    @Insert
    suspend fun insert(entity: ImportRecordEntity): Long

    @Query("SELECT * FROM import_records WHERE id = :id")
    suspend fun findById(id: Long): ImportRecordEntity?

    @Query(
        "SELECT * FROM import_records ORDER BY created_at DESC, id DESC",
    )
    fun observeAll(): Flow<List<ImportRecordEntity>>

    @Query(
        "DELETE FROM import_records WHERE id = :id AND NOT EXISTS " +
            "(SELECT 1 FROM vacancies WHERE import_record_id = :id)",
    )
    suspend fun deleteUnused(id: Long): Int
}
