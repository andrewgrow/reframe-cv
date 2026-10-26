package reframecv.runners

import reframecv.domain.models.providers.RunnerRawResult
import reframecv.domain.models.providers.RunnerResult
import reframecv.domain.models.vacancy.Vacancy

/** Lets each supported operation return its own normalized result type. */
sealed interface RunnerResultItem {
    val externalId: String
    val title: String
}

/** Represents a job search result before the user decides to add it to a project. */
data class VacancySearchResult(
    override val externalId: String,
    override val title: String,
    val company: String = "",
    val description: String = "",
    val url: String,
    val location: String = "",
    val postedAt: String? = null,
    val salary: String = "",
    val employmentType: String = "",
    val seniorityLevel: String = "",
    val source: String = "LinkedIn",
) : RunnerResultItem {
    fun toVacancy(projectId: Long, timestamp: Long) = Vacancy(
        projectId = projectId,
        name = title,
        jobTitle = title,
        company = company,
        description = description,
        url = url,
        createdAt = timestamp,
        updatedAt = timestamp,
    )
}

/** Keeps provider pagination independent of how many malformed or duplicate rows were skipped. */
data class RunnerResultsPage(
    val items: List<RunnerResultItem>,
    val offset: Int,
    val nextOffset: Int?,
    val total: Int? = null,
    val skipped: Int = 0,
    val rawResults: List<RunnerRawResult> = emptyList(),
    val savedResults: List<RunnerResult> = emptyList(),
    val savedCount: Int = savedResults.size,
    val visibleCount: Int = savedCount,
)
