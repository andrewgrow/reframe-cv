package reframecv.providers.apify

import kotlinx.serialization.json.Json
import reframecv.domain.models.providers.RunnerResult
import reframecv.runners.RunnerResultConverter

/** Maps stored Apify output into a vacancy without losing its original snapshot. */
class ApifyResultConverter : RunnerResultConverter {
    private val json = Json { ignoreUnknownKeys = true }
    override fun vacancy(result: RunnerResult, projectId: Long, timestamp: Long) = run {
        val mapped = when (result.source) {
            "LinkedIn" -> json.decodeFromString<ApifyVacancyDto>(result.rawResultAsJson).toDomain()
            "AMS" -> json.decodeFromString<AmsVacancyDto>(result.rawResultAsJson).toDomain()
            else -> throw IllegalArgumentException("Unsupported result source")
        }
        val candidate = checkNotNull(mapped) {
            "This result does not contain an identifiable job posting."
        }
        candidate.toVacancy(projectId, timestamp)
    }
}
