package reframecv.runners

import reframecv.domain.models.providers.RunnerResult
import reframecv.domain.models.vacancy.Vacancy

/** Converts supported source snapshots without contacting the provider again. */
fun interface RunnerResultConverter {
    fun vacancy(result: RunnerResult, projectId: Long, timestamp: Long): Vacancy
}
