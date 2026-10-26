package reframecv.database.providers

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.serialization.json.Json
import reframecv.domain.models.providers.ConnectionRun

/** Keeps snapshots immutable and resolves earlier source identities across all runners. */
@Dao
abstract class RunnerResultsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(rows: List<RunnerResultEntity>)

    @Query(
        "SELECT * FROM runner_results WHERE run_id = :runId AND position >= :offset " +
            "AND position < :end ORDER BY position",
    )
    abstract suspend fun page(runId: Long, offset: Int, end: Int): List<RunnerResultEntity>

    @Query("SELECT COUNT(*) FROM runner_results r WHERE run_id = :runId AND $REVIEW_FILTER")
    abstract suspend fun count(
        runId: Long,
        showDismissed: Boolean = true,
        showImported: Boolean = true,
    ): Int

    @Query(
        "SELECT r.* FROM runner_results r WHERE run_id = :runId AND " + REVIEW_FILTER +
            " ORDER BY position LIMIT :limit OFFSET :offset",
    )
    abstract suspend fun reviewPage(
        runId: Long,
        offset: Int,
        limit: Int,
        showDismissed: Boolean = true,
        showImported: Boolean = true,
    ): List<RunnerResultEntity>

    @Query(
        "UPDATE connection_runs SET next_result_offset = :offset, result_total = :total, " +
            "results_complete = :complete, result_columns = :columns WHERE id = :id " +
            "AND next_result_offset <= :offset",
    )
    abstract suspend fun cursor(
        id: Long,
        offset: Int,
        total: Int?,
        complete: Boolean,
        columns: String,
    ): Int

    @Transaction
    open suspend fun commit(run: ConnectionRun, rows: List<RunnerResultEntity>) {
        insert(rows)
        check(
            cursor(
                run.id,
                run.nextResultOffset,
                run.resultTotal,
                run.resultsComplete,
                Json.encodeToString(run.resultColumns),
            ) == 1,
        )
    }

    @Query(
        "UPDATE runner_results SET relevance_score = :score, relevance_prompt = :prompt " +
            "WHERE id = :id AND run_id = :runId",
    )
    abstract suspend fun setRelevance(id: Long, runId: Long, score: Int, prompt: String): Int

    @Query("SELECT * FROM runner_results WHERE id = :id")
    abstract suspend fun get(id: Long): RunnerResultEntity?

    @Query(
        "SELECT * FROM runner_results WHERE source = :source AND external_id = :externalId " +
            "AND run_id < :runId ORDER BY (status = 'Imported') DESC, run_id DESC, id DESC LIMIT 1",
    )
    abstract suspend fun previous(
        source: String,
        externalId: String,
        runId: Long,
    ): RunnerResultEntity?

    @Query("UPDATE runner_results SET status = :status, vacancy_id = :vacancyId WHERE id = :id")
    abstract suspend fun writeStatus(id: Long, status: String, vacancyId: Long?): Int

    @Transaction
    open suspend fun setStatus(id: Long, status: String, vacancyId: Long?) {
        val current = checkNotNull(get(id))
        // Imported remains historical even if its vacancy was subsequently removed.
        require(current.status != "Imported" || status == "Imported")
        check(writeStatus(id, status, vacancyId) == 1)
    }
}

private const val REVIEW_STATUS =
    "CASE WHEN r.status != 'New' THEN r.status ELSE COALESCE(" +
        "(SELECT p.status FROM runner_results p WHERE p.source = r.source " +
        "AND p.external_id = r.external_id AND p.run_id < r.run_id " +
        "ORDER BY (p.status = 'Imported') DESC, p.run_id DESC, p.id DESC LIMIT 1), 'New') END"
private const val REVIEW_FILTER =
    "(:showDismissed OR (" + REVIEW_STATUS + ") != 'Dismissed') AND " +
        "(:showImported OR (" + REVIEW_STATUS + ") != 'Imported')"
