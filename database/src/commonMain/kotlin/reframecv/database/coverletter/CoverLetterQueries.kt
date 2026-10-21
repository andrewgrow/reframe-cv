package reframecv.database.coverletter

import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import reframecv.database.vacancy.VacancyLetterEntity

/** Provides SQL queries for cover letter records and keywords. */
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
        "SELECT * FROM cover_letters WHERE project_id = :projectId ORDER BY id ASC",
    )
    fun observeActive(projectId: Long): Flow<List<CoverLetterWithKeywords>>

    @Query("SELECT COUNT(*) FROM cover_letters WHERE project_id = :projectId")
    fun observeCount(projectId: Long): Flow<Int>

    @Transaction
    @Query(
        "SELECT * FROM cover_letters WHERE project_id = :projectId AND " +
            "EXISTS (SELECT 1 FROM cover_letters_keywords WHERE record_id = cover_letters.id " +
            "AND normalized_keyword = :keyword) ORDER BY id ASC",
    )
    suspend fun searchNormalized(projectId: Long, keyword: String): List<CoverLetterWithKeywords>

    @Query("DELETE FROM cover_letters WHERE id = :id")
    suspend fun deleteEntity(id: Long): Int
}
