package reframecv.database.vacancy

import androidx.room3.Query

/** Records applications without replacing dates already assigned to vacancies. */
interface VacancyApplicationQueries {
    @Query(
        "UPDATE vacancies SET applied_at = :timestamp, updated_at = :timestamp " +
            "WHERE project_id = :projectId AND id IN (:ids) AND applied_at IS NULL",
    )
    suspend fun markApplied(projectId: Long, ids: List<Long>, timestamp: Long): Int
}
