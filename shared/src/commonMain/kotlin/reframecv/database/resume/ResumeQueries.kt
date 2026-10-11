package reframecv.database.resume

import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** Provides SQL queries for resume records and keywords. */
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
        "SELECT * FROM resumes WHERE project_id = :projectId ORDER BY updated_at DESC, name ASC, id ASC",
    )
    fun observeActive(projectId: Long): Flow<List<ResumeWithKeywords>>

    @Query("SELECT COUNT(*) FROM resumes WHERE project_id = :projectId")
    fun observeCount(projectId: Long): Flow<Int>

    @Transaction
    @Query(
        "SELECT * FROM resumes WHERE project_id = :projectId AND " +
            "EXISTS (SELECT 1 FROM resumes_keywords WHERE record_id = resumes.id " +
            "AND normalized_keyword = :keyword) ORDER BY updated_at DESC, name ASC, id ASC",
    )
    suspend fun searchNormalized(projectId: Long, keyword: String): List<ResumeWithKeywords>

    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteEntity(id: Long): Int
}
