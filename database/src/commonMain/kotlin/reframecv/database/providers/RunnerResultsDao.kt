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
abstract class RunnerResultsDao : RunnerResultReviewQueries {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(rows: List<RunnerResultEntity>)

    @Query(
        "SELECT * FROM runner_results WHERE run_id = :runId AND position >= :offset " +
            "AND position < :end ORDER BY position",
    )
    abstract suspend fun page(runId: Long, offset: Int, end: Int): List<RunnerResultEntity>

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
        "SELECT p.id, p.status, p.vacancy_id, " +
            "(SELECT a.relevance_score" + LAST_RELEVANCE + ") AS relevance_score, " +
            "(SELECT a.relevance_prompt" + LAST_RELEVANCE + ") AS relevance_prompt " +
            "FROM runner_results p WHERE p.source = :source AND p.external_id = :externalId " +
            "AND p.run_id != :runId AND (p.run_id < :runId OR p.status = 'Imported') " +
            "ORDER BY " + PREVIOUS_RESULT_ORDER + " LIMIT 1",
    )
    abstract suspend fun previous(
        source: String,
        externalId: String,
        runId: Long,
    ): RunnerResultHistory?

    @Query("UPDATE runner_results SET status = :status, vacancy_id = :vacancyId WHERE id = :id")
    abstract suspend fun writeStatus(id: Long, status: String, vacancyId: Long?): Int

    @Transaction
    open suspend fun setStatus(id: Long, status: String, vacancyId: Long?) {
        val current = checkNotNull(get(id))
        // Imported remains historical even if its vacancy was subsequently removed.
        val previous = current.externalId?.let { previous(current.source, it, current.runId) }
        val imported = current.status == "Imported" || previous?.status == "Imported" ||
            existingVacancy(id) != null
        require(!imported || status == "Imported")
        check(writeStatus(id, status, vacancyId) == 1)
    }
}
