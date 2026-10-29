package reframecv.database.providers

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import reframecv.database.vacancy.VacancyDao
import reframecv.domain.models.vacancy.Vacancy

/** Creates a vacancy and records its source import in one database transaction. */
@Dao
interface RunnerResultImportsDao :
    VacancyDao,
    RunnerVacancyMatchQueries {
    @Query("SELECT * FROM runner_results WHERE id = :id")
    suspend fun sourceResult(id: Long): RunnerResultEntity?

    @Query("UPDATE runner_results SET status = 'Imported', vacancy_id = :vacancyId WHERE id = :id")
    suspend fun linkImport(id: Long, vacancyId: Long): Int

    @Query(
        "SELECT COUNT(*) FROM runner_results r WHERE id = :id AND (" + RECORDED_REVIEW_STATUS +
            ") NOT IN ('Imported', 'Dismissed')",
    )
    suspend fun importable(id: Long): Int

    @Transaction
    suspend fun importResult(id: Long, vacancy: Vacancy): Long {
        require(importable(id) == 1 && existingVacancy(id) == null)
        require(vacancy.id == 0L)
        val saved = create(vacancy)
        check(linkImport(id, saved) == 1)
        return saved
    }
}
