package reframecv.repository

import reframecv.domain.models.providers.RunnerResult
import reframecv.domain.models.providers.RunnerResultStatus

internal fun runnerResultWithHistory(row: RunnerResult, records: List<RunnerResult>): RunnerResult {
    val history = records.filter {
        row.externalId != null && it.source == row.source && it.externalId == row.externalId &&
            it.runId != row.runId &&
            (it.runId < row.runId || it.status == RunnerResultStatus.Imported)
    }
    val previous = history.sortedWith(
        compareByDescending<RunnerResult> { it.status == RunnerResultStatus.Imported }
            .thenByDescending { it.status != RunnerResultStatus.New }
            .thenByDescending { it.runId }.thenByDescending { it.id },
    ).firstOrNull()
    val assessment = history.filter { it.runId < row.runId && it.relevanceScore != null }
        .maxWithOrNull(compareBy<RunnerResult> { it.runId }.thenBy { it.id })
    return row.copy(
        previousResultId = previous?.id,
        previousStatus = previous?.status,
        previousVacancyId = previous?.vacancyId,
        relevanceScore = row.relevanceScore ?: assessment?.relevanceScore,
        relevancePrompt = if (row.relevanceScore !=
            null
        ) {
            row.relevancePrompt
        } else {
            assessment?.relevancePrompt
        },
    )
}
