package reframecv.database.vacancyattachment

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Stores vacancy attachments and removes metadata after its last vacancy reference. */
@Dao
interface VacancyAttachmentDao {
    @Query("SELECT file_path FROM vacancy_attachments")
    suspend fun filePaths(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM vacancy_attachments WHERE file_path = :path)")
    suspend fun referencesFile(path: String): Boolean

    @Insert
    suspend fun insert(entity: VacancyAttachmentEntity): Long

    @Query("SELECT * FROM vacancy_attachments WHERE id = :id")
    suspend fun findById(id: Long): VacancyAttachmentEntity?

    @Query(
        "SELECT * FROM vacancy_attachments ORDER BY created_at DESC, id DESC",
    )
    fun observeAll(): Flow<List<VacancyAttachmentEntity>>

    @Query(
        "DELETE FROM vacancy_attachments WHERE id = :id AND NOT EXISTS " +
            "(SELECT 1 FROM vacancies WHERE vacancy_attachment_id = :id)",
    )
    suspend fun deleteUnused(id: Long): Int
}
