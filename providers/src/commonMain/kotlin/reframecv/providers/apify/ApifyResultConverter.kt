package reframecv.providers.apify

import kotlinx.serialization.json.Json
import reframecv.domain.models.providers.RunnerResult
import reframecv.runners.RunnerResultConverter

/** Maps stored LinkedIn output into a vacancy without losing its original snapshot. */
class ApifyResultConverter : RunnerResultConverter {
    private val json = Json { ignoreUnknownKeys = true }
    override fun vacancy(result: RunnerResult, projectId: Long, timestamp: Long) = run {
        require(result.source == "LinkedIn") { "Unsupported result source" }
        val candidate =
            checkNotNull(
                json.decodeFromString<ApifyVacancyDto>(result.rawResultAsJson).toDomain(),
            ) {
                "This result does not contain an identifiable job posting."
            }
        candidate.toVacancy(projectId, timestamp)
    }
}
