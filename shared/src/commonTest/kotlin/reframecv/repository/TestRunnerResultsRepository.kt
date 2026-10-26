package reframecv.repository

import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.RunnerRawResult
import reframecv.domain.models.providers.RunnerResult
import reframecv.domain.models.providers.RunnerResultFilter
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.domain.models.vacancy.Vacancy

class TestRunnerResultsRepository(
    private val committed: suspend (ConnectionRun) -> Unit = {},
    private val createVacancy: suspend (Vacancy) -> Long = { error("No vacancy writer") },
) : RunnerResultsRepository {
    val records = mutableListOf<RunnerResult>()
    var failRelevanceSave = false
    override suspend fun setRelevance(id: Long, runId: Long, score: Int, prompt: String) {
        check(!failRelevanceSave)
        val index = records.indexOfFirst { it.id == id && it.runId == runId }
        check(index >= 0)
        records[index] = records[index].copy(relevanceScore = score, relevancePrompt = prompt)
    }
    override suspend fun get(id: Long) = records.find { it.id == id }?.let {
        runnerResultWithHistory(it, records)
    }
    override suspend fun count(runId: Long, filter: RunnerResultFilter) =
        page(runId, 0, Int.MAX_VALUE).count(filter::includes)
    override suspend fun reviewPage(
        runId: Long,
        offset: Int,
        limit: Int,
        filter: RunnerResultFilter,
    ) = page(runId, 0, Int.MAX_VALUE).filter(filter::includes)
        .sortedBy { it.position }.drop(offset).take(limit)
    override suspend fun commitPage(
        run: ConnectionRun,
        rows: List<RunnerRawResult>,
        receivedAt: Long,
    ) {
        savePage(run.id, rows, receivedAt)
        committed(run)
    }
    override suspend fun importVacancy(id: Long, vacancy: Vacancy): Long {
        require(get(id)?.reviewStatus == RunnerResultStatus.New)
        val saved = createVacancy(vacancy)
        setStatus(id, RunnerResultStatus.Imported, saved)
        return saved
    }
    override suspend fun savePage(runId: Long, rows: List<RunnerRawResult>, receivedAt: Long) {
        rows.forEach { raw ->
            val exists = records.any {
                it.runId == runId && (
                    it.position == raw.position ||
                        (
                            raw.externalId != null && it.source == raw.source &&
                                it.externalId == raw.externalId
                            )
                    )
            }
            if (!exists) {
                records += RunnerResult(
                    (records.maxOfOrNull { it.id } ?: 0) + 1,
                    runId,
                    raw.position,
                    raw.source,
                    raw.externalId,
                    raw.rawResultAsJson,
                    receivedAt,
                )
            }
        }
    }
    override suspend fun page(runId: Long, offset: Int, limit: Int) = records.filter {
        it.runId == runId && it.position in offset until offset + limit
    }.map { runnerResultWithHistory(it, records) }
    override suspend fun setStatus(id: Long, status: RunnerResultStatus, vacancyId: Long?) {
        val current = checkNotNull(get(id))
        require(
            current.reviewStatus != RunnerResultStatus.Imported ||
                status == RunnerResultStatus.Imported,
        )
        val index = records.indexOfFirst { it.id == id }
        check(index >= 0)
        records[index] = records[index].copy(status = status, vacancyId = vacancyId)
    }
}
