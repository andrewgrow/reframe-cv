package reframecv.database.providers

import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.serialization.json.Json

/** Applies the same resolved posting identities before counting and selecting local result pages. */
interface RunnerResultReviewQueries : RunnerVacancyMatchQueries {
    @Query("SELECT COUNT(*) FROM runner_results r WHERE run_id = :runId AND $REVIEW_FILTER")
    suspend fun reviewCount(
        runId: Long,
        existingResultIds: String,
        showDismissed: Boolean,
        showImported: Boolean,
    ): Int

    @Query(
        "SELECT r.* FROM runner_results r WHERE run_id = :runId AND " + REVIEW_FILTER +
            " ORDER BY position LIMIT :limit OFFSET :offset",
    )
    suspend fun selectReviewPage(
        runId: Long,
        offset: Int,
        limit: Int,
        existingResultIds: String,
        showDismissed: Boolean,
        showImported: Boolean,
    ): List<RunnerResultEntity>

    @Transaction
    suspend fun count(
        runId: Long,
        showDismissed: Boolean = true,
        showImported: Boolean = true,
    ): Int = reviewCount(
        runId,
        Json.encodeToString(existingVacancies(runId).keys.toList()),
        showDismissed,
        showImported,
    )

    @Transaction
    suspend fun reviewPage(
        runId: Long,
        offset: Int,
        limit: Int,
        showDismissed: Boolean = true,
        showImported: Boolean = true,
    ): List<RunnerResultEntity> = selectReviewPage(
        runId,
        offset,
        limit,
        Json.encodeToString(existingVacancies(runId).keys.toList()),
        showDismissed,
        showImported,
    )
}
