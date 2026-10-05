package reframecv.database.coverletter

import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** Low-level storage queries; use the DAO's transactional methods for writes. */
interface CoverLetterQueries {
    @Insert
    suspend fun insertEntity(entity: CoverLetterEntity): Long

    @Update
    suspend fun updateEntity(entity: CoverLetterEntity): Int

    @Insert
    suspend fun insertKeywords(keywords: List<CoverLetterKeywordEntity>)

    @Query("DELETE FROM cover_letters_keywords WHERE record_id = :id")
    suspend fun clearKeywords(id: Long)

    @Transaction
    @Query("SELECT * FROM cover_letters WHERE id = :id")
    suspend fun findById(id: Long): CoverLetterWithKeywords?

    @Transaction
    @Query(
        "SELECT * FROM cover_letters WHERE project_id = :projectId AND " +
            "cover_letters.deleted_at IS NULL AND EXISTS (SELECT 1 FROM " +
            "projects WHERE projects.id = cover_letters.project_id AND " +
            "projects.deleted_at IS NULL) " +
            "ORDER BY updated_at DESC, name ASC, id ASC",
    )
    fun observeActive(projectId: Long): Flow<List<CoverLetterWithKeywords>>

    @Query(
        "SELECT COUNT(*) FROM cover_letters WHERE project_id = :projectId AND " +
            "cover_letters.deleted_at IS NULL AND EXISTS (SELECT 1 FROM " +
            "projects WHERE projects.id = cover_letters.project_id AND " +
            "projects.deleted_at IS NULL)",
    )
    fun observeCount(projectId: Long): Flow<Int>

    @Transaction
    @Query(
        "SELECT * FROM cover_letters WHERE project_id = :projectId AND " +
            "cover_letters.deleted_at IS NULL AND EXISTS (SELECT 1 FROM " +
            "projects WHERE projects.id = cover_letters.project_id AND " +
            "projects.deleted_at IS NULL) AND " +
            "EXISTS (SELECT 1 FROM cover_letters_keywords WHERE record_id = cover_letters.id " +
            "AND normalized_keyword = :keyword) ORDER BY updated_at DESC, name ASC, id ASC",
    )
    suspend fun searchNormalized(projectId: Long, keyword: String): List<CoverLetterWithKeywords>

    @Query(
        "UPDATE cover_letters SET deleted_at = :deletedAt, updated_at = " +
            ":deletedAt WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun markDeleted(id: Long, deletedAt: Long): Int
}
