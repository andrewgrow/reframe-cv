package reframecv.usecase

import kotlinx.coroutines.flow.map
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.repository.ProjectsRepository
import reframecv.repository.RunnerResultsRepository
import reframecv.runners.RunnerResultConverter
import reframecv.shared.nowMillis

/** Reviews source snapshots and imports supported rows into eligible project workspaces. */
class RunnerResultReviewUseCases(
    private val results: RunnerResultsRepository,
    private val projects: ProjectsRepository,
    private val converter: RunnerResultConverter,
) {
    fun projects() = projects.observeAllProjects().map { all ->
        val parents = all.mapNotNull { it.parentId }.toSet()
        all.filter { it.id !in parents }.sortedBy { it.id }
    }
    suspend fun review(runId: Long, id: Long, dismiss: Boolean) {
        val result = checkNotNull(results.get(id))
        require(result.runId == runId && result.reviewStatus != RunnerResultStatus.Imported)
        results.setStatus(
            id,
            if (dismiss) RunnerResultStatus.Dismissed else RunnerResultStatus.Restored,
        )
    }
    suspend fun import(runId: Long, id: Long, projectId: Long): Long {
        val result = checkNotNull(results.get(id))
        require(result.runId == runId && result.reviewStatus == RunnerResultStatus.New)
        return results.importVacancy(id, converter.vacancy(result, projectId, nowMillis()))
    }
}
