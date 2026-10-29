package reframecv.database.providers

import androidx.room3.ColumnInfo

/** Combines recorded imports or earlier review decisions with the latest earlier assessment. */
data class RunnerResultHistory(
    val id: Long,
    val status: String,
    @ColumnInfo(name = "vacancy_id") val vacancyId: Long?,
    @ColumnInfo(name = "relevance_score") val relevanceScore: Int?,
    @ColumnInfo(name = "relevance_prompt") val relevancePrompt: String?,
)

internal const val PREVIOUS_RESULT_ORDER =
    "(p.status = 'Imported') DESC, (p.status != 'New') DESC, p.run_id DESC, p.id DESC"

internal const val RECORDED_REVIEW_STATUS =
    "CASE WHEN r.status = 'Imported' OR EXISTS(" +
        "SELECT 1 FROM runner_results p WHERE p.source = r.source " +
        "AND p.external_id = r.external_id AND p.status = 'Imported') " +
        "THEN 'Imported' WHEN r.status != 'New' THEN r.status ELSE COALESCE(" +
        "(SELECT p.status FROM runner_results p WHERE p.source = r.source " +
        "AND p.external_id = r.external_id AND p.run_id < r.run_id " +
        "ORDER BY " + PREVIOUS_RESULT_ORDER + " LIMIT 1), 'New') END"
internal const val REVIEW_STATUS =
    "CASE WHEN r.id IN (SELECT value FROM json_each(:existingResultIds)) THEN 'Imported' " +
        "ELSE ($RECORDED_REVIEW_STATUS) END"
internal const val REVIEW_FILTER =
    "(:showDismissed OR (" + REVIEW_STATUS + ") != 'Dismissed') AND " +
        "(:showImported OR (" + REVIEW_STATUS + ") != 'Imported')"

internal const val LAST_RELEVANCE =
    " FROM runner_results a WHERE a.source = :source AND a.external_id = :externalId " +
        "AND a.run_id < :runId AND a.relevance_score IS NOT NULL " +
        "ORDER BY a.run_id DESC, a.id DESC LIMIT 1"
