package reframecv.repository

import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.RunnerRawResult
import reframecv.domain.models.providers.RunnerResult
import reframecv.domain.models.providers.RunnerResultFilter
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.domain.models.vacancy.Vacancy

/** Stores source snapshots once and finds matching identities from earlier runs. */
interface RunnerResultsRepository {
    suspend fun get(id: Long): RunnerResult?
    suspend fun count(runId: Long, filter: RunnerResultFilter = RunnerResultFilter()): Int
    suspend fun reviewPage(
        runId: Long,
        offset: Int,
        limit: Int,
        filter: RunnerResultFilter = RunnerResultFilter(),
    ): List<RunnerResult>
    suspend fun commitPage(run: ConnectionRun, rows: List<RunnerRawResult>, receivedAt: Long)
    suspend fun importVacancy(id: Long, vacancy: Vacancy): Long
    suspend fun savePage(runId: Long, rows: List<RunnerRawResult>, receivedAt: Long)
    suspend fun page(runId: Long, offset: Int, limit: Int): List<RunnerResult>
    suspend fun setStatus(id: Long, status: RunnerResultStatus, vacancyId: Long? = null)
}
