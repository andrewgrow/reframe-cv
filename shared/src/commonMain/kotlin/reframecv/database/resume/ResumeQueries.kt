package reframecv.database.resume

import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** Low-level storage queries; use the DAO's transactional methods for writes. */
interface ResumeQueries {
    @Insert
    suspend fun insertEntity(entity: ResumeEntity): Long

    @Update
    suspend fun updateEntity(entity: ResumeEntity): Int

    @Insert
    suspend fun insertKeywords(keywords: List<ResumeKeywordEntity>)

    @Query("DELETE FROM resumes_keywords WHERE record_id = :id")
    suspend fun clearKeywords(id: Long)

    @Transaction
    @Query("SELECT * FROM resumes WHERE id = :id")
    suspend fun findById(id: Long): ResumeWithKeywords?

    @Transaction
    @Query(
        "SELECT * FROM resumes WHERE project_id = :projectId AND " +
            "resumes.deleted_at IS NULL AND EXISTS (SELECT 1 FROM projects " +
            "WHERE projects.id = resumes.project_id AND projects.deleted_at IS NULL) " +
            "ORDER BY updated_at DESC, name ASC, id ASC",
    )
    fun observeActive(projectId: Long): Flow<List<ResumeWithKeywords>>

    @Query(
        "SELECT COUNT(*) FROM resumes WHERE project_id = :projectId AND " +
            "resumes.deleted_at IS NULL AND EXISTS (SELECT 1 FROM projects " +
            "WHERE projects.id = resumes.project_id AND projects.deleted_at IS NULL)",
    )
    fun observeCount(projectId: Long): Flow<Int>

    @Transaction
    @Query(
        "SELECT * FROM resumes WHERE project_id = :projectId AND " +
            "resumes.deleted_at IS NULL AND EXISTS (SELECT 1 FROM projects " +
            "WHERE projects.id = resumes.project_id AND projects.deleted_at IS NULL) AND " +
            "EXISTS (SELECT 1 FROM resumes_keywords WHERE record_id = resumes.id " +
            "AND normalized_keyword = :keyword) ORDER BY updated_at DESC, name ASC, id ASC",
    )
    suspend fun searchNormalized(projectId: Long, keyword: String): List<ResumeWithKeywords>

    @Query(
        "UPDATE resumes SET deleted_at = :deletedAt, updated_at = :deletedAt " +
            "WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun markDeleted(id: Long, deletedAt: Long): Int
}
