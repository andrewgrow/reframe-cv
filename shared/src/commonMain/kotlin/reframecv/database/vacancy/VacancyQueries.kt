package reframecv.database.vacancy

import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** Low-level storage queries; use the DAO's transactional methods for writes. */
interface VacancyQueries {
    @Insert
    suspend fun insertEntity(entity: VacancyEntity): Long

    @Update
    suspend fun updateEntity(entity: VacancyEntity): Int

    @Insert
    suspend fun insertKeywords(keywords: List<VacancyKeywordEntity>)

    @Query("DELETE FROM vacancies_keywords WHERE record_id = :id")
    suspend fun clearKeywords(id: Long)

    @Transaction
    @Query("SELECT * FROM vacancies WHERE id = :id")
    suspend fun findById(id: Long): VacancyWithKeywords?

    @Transaction
    @Query(
        "SELECT * FROM vacancies WHERE project_id = :projectId AND " +
            "EXISTS (SELECT 1 FROM projects " +
            "WHERE projects.id = vacancies.project_id AND projects.deleted_at IS NULL) " +
            "ORDER BY updated_at DESC, name ASC, id ASC",
    )
    fun observeActive(projectId: Long): Flow<List<VacancyWithKeywords>>

    @Query(
        "SELECT COUNT(*) FROM vacancies WHERE project_id = :projectId AND " +
            "EXISTS (SELECT 1 FROM projects " +
            "WHERE projects.id = vacancies.project_id AND projects.deleted_at IS NULL)",
    )
    fun observeCount(projectId: Long): Flow<Int>

    @Transaction
    @Query(
        "SELECT * FROM vacancies WHERE project_id = :projectId AND " +
            "EXISTS (SELECT 1 FROM projects " +
            "WHERE projects.id = vacancies.project_id AND projects.deleted_at IS NULL) AND " +
            "EXISTS (SELECT 1 FROM vacancies_keywords WHERE record_id = vacancies.id " +
            "AND normalized_keyword = :keyword) ORDER BY updated_at DESC, name ASC, id ASC",
    )
    suspend fun searchNormalized(projectId: Long, keyword: String): List<VacancyWithKeywords>

    @Query("DELETE FROM vacancies WHERE id = :id")
    suspend fun deleteEntity(id: Long): Int

    @Query(
        "DELETE FROM import_records WHERE id = :id AND NOT EXISTS " +
            "(SELECT 1 FROM vacancies WHERE import_record_id = :id)",
    )
    suspend fun deleteUnusedImportRecord(id: Long): Int
}
