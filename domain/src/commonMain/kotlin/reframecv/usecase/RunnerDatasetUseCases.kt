package reframecv.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ConnectionRunState
import reframecv.domain.models.providers.RunnerResultFilter
import reframecv.repository.ConnectionRunnersRepository
import reframecv.repository.ProviderConnectionsRepository
import reframecv.repository.RunnerResultsRepository
import reframecv.runners.RunnerExecutionRepository
import reframecv.runners.RunnerResultsPage
import reframecv.shared.nowMillis

/** Separates local review pages from resumable reads of the remote dataset. */
class RunnerDatasetUseCases(
    private val runners: ConnectionRunnersRepository,
    private val connections: ProviderConnectionsRepository,
    private val remote: RunnerExecutionRepository,
    private val results: RunnerResultsRepository,
    private val now: () -> Long = ::nowMillis,
) {
    private val loading = Mutex()
    suspend fun page(
        runnerId: Long,
        runId: Long,
        offset: Int = 0,
        filter: RunnerResultFilter = RunnerResultFilter(),
    ): RunnerRunView {
        require(offset >= 0)
        val run = checkNotNull(runners.getRun(runId))
        require(run.runnerId == runnerId)
        val count = results.count(runId)
        val visibleCount = results.count(runId, filter)
        val lastOffset = ((visibleCount - 1).coerceAtLeast(0) / REVIEW_PAGE_SIZE) * REVIEW_PAGE_SIZE
        val currentOffset = offset.coerceAtMost(lastOffset)
        val saved = results.reviewPage(runId, currentOffset, REVIEW_PAGE_SIZE, filter)
        val page = RunnerResultsPage(
            emptyList(),
            currentOffset,
            (currentOffset + REVIEW_PAGE_SIZE).takeIf { it < visibleCount },
            run.resultTotal,
            savedResults = saved,
            savedCount = count,
            visibleCount = visibleCount,
        )
        return RunnerRunView(run, null, page)
    }
    suspend fun loadMore(
        runnerId: Long,
        runId: Long,
        offset: Int = 0,
        filter: RunnerResultFilter = RunnerResultFilter(),
    ): RunnerRunView = loading.withLock {
        val run = checkNotNull(runners.getRun(runId))
        require(run.runnerId == runnerId && run.state == ConnectionRunState.Succeeded)
        if (!run.resultsComplete) {
            val runner = checkNotNull(runners.get(runnerId))
            val connection = checkNotNull(connections.get(runner.connectionId))
            require(
                connection.active && connection.usable &&
                    connection.type == ApiProviderType.Apify,
            ) {
                "Provider connection is disabled or not configured."
            }
            val response = remote.results(
                connection,
                checkNotNull(run.datasetId),
                run.nextResultOffset,
                DOWNLOAD_PAGE_SIZE,
                runner.operation,
            )
            require(response.offset == run.nextResultOffset)
            val consumed = response.rawResults.size
            require(response.nextOffset == null || response.nextOffset > run.nextResultOffset)
            val next = response.nextOffset ?: (run.nextResultOffset + consumed)
            val keys = response.rawResults.flatMap {
                val value = Json.parseToJsonElement(it.rawResultAsJson)
                if (value is JsonObject) value.keys.toList() else listOf("value")
            }
            val updated = run.copy(
                nextResultOffset = next,
                resultTotal =
                    response.total ?: run.resultTotal,
                resultsComplete = response.nextOffset == null,
                resultColumns = (run.resultColumns + keys).distinct(),
            )
            results.commitPage(updated, response.rawResults, now())
        }
        page(runnerId, runId, offset, filter)
    }
    companion object {
        const val REVIEW_PAGE_SIZE = 25
        const val DOWNLOAD_PAGE_SIZE = 50
    }
}
