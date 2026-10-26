package reframecv.repository

import reframecv.database.providers.RunnerResultEntity
import reframecv.database.providers.RunnerResultImportsDao
import reframecv.database.providers.RunnerResultsDao
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.RunnerRawResult
import reframecv.domain.models.providers.RunnerResultFilter
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.domain.models.vacancy.Vacancy

/** Persists source snapshots and attaches historical import information to result previews. */
class LocalRunnerResultsRepository(
    private val dao: RunnerResultsDao,
    private val imports: RunnerResultImportsDao,
) : RunnerResultsRepository {
    override suspend fun savePage(runId: Long, rows: List<RunnerRawResult>, receivedAt: Long) {
        dao.insert(entities(runId, rows, receivedAt))
    }
    override suspend fun setRelevance(id: Long, runId: Long, score: Int, prompt: String) {
        require(score in 0..MAX_RELEVANCE_SCORE && prompt.isNotBlank())
        check(dao.setRelevance(id, runId, score, prompt) == 1)
    }
    override suspend fun get(id: Long) = dao.get(id)?.let { enrich(listOf(it)).single() }
    override suspend fun count(runId: Long, filter: RunnerResultFilter) =
        dao.count(runId, filter.showDismissed, filter.showImported)
    override suspend fun reviewPage(
        runId: Long,
        offset: Int,
        limit: Int,
        filter: RunnerResultFilter,
    ) = enrich(dao.reviewPage(runId, offset, limit, filter.showDismissed, filter.showImported))
    override suspend fun commitPage(
        run: ConnectionRun,
        rows: List<RunnerRawResult>,
        receivedAt: Long,
    ) = dao.commit(run, entities(run.id, rows, receivedAt))
    override suspend fun importVacancy(id: Long, vacancy: Vacancy) =
        imports.importResult(id, vacancy)
    private fun entities(
        runId: Long,
        rows: List<RunnerRawResult>,
        receivedAt: Long,
    ): List<RunnerResultEntity> {
        require(rows.all { it.position >= 0 && it.source.isNotBlank() })
        return rows.map {
            RunnerResultEntity(
                runId = runId,
                position = it.position,
                source = it.source,
                externalId = it.externalId,
                rawResultAsJson = it.rawResultAsJson,
                receivedAt = receivedAt,
            )
        }
    }
    override suspend fun page(runId: Long, offset: Int, limit: Int) =
        enrich(dao.page(runId, offset, offset + limit))
    private suspend fun enrich(rows: List<RunnerResultEntity>) = rows.map { row ->
        val previous = row.externalId?.let { dao.previous(row.source, it, row.runId) }
        row.toDomainModel().copy(
            previousResultId = previous?.id,
            previousStatus = previous?.status?.let(RunnerResultStatus::valueOf),
            previousVacancyId = previous?.vacancyId,
            relevanceScore = row.relevanceScore ?: previous?.relevanceScore,
            relevancePrompt = if (row.relevanceScore !=
                null
            ) {
                row.relevancePrompt
            } else {
                previous?.relevancePrompt
            },
        )
    }
    override suspend fun setStatus(id: Long, status: RunnerResultStatus, vacancyId: Long?) {
        require((status == RunnerResultStatus.Imported) == (vacancyId != null))
        dao.setStatus(id, status.name, vacancyId)
    }
}

private const val MAX_RELEVANCE_SCORE = 100
