package reframecv.database.importrecord

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Source payloads are immutable and can be physically deleted when no vacancy uses them. */
@Dao
interface ImportRecordDao {
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
