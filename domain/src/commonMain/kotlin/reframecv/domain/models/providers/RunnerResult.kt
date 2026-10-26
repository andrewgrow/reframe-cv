package reframecv.domain.models.providers

import reframecv.domain.DomainModel

/** Tracks the user's decision independently of the original provider payload. */
enum class RunnerResultStatus { New, Imported, Dismissed, Restored }

/** Represents one immutable source snapshot and its local review state. */
data class RunnerResult(
    val id: Long,
    val runId: Long,
    val position: Int,
    val source: String,
    val externalId: String?,
    val rawResultAsJson: String,
    val receivedAt: Long,
    val status: RunnerResultStatus = RunnerResultStatus.New,
    val vacancyId: Long? = null,
    val previousResultId: Long? = null,
    val previousStatus: RunnerResultStatus? = null,
    val previousVacancyId: Long? = null,
    val relevanceScore: Int? = null,
    val relevancePrompt: String? = null,
) : DomainModel {
    val reviewStatus: RunnerResultStatus get() {
        val imported = RunnerResultStatus.Imported
        val decision = when {
            status == imported || previousStatus == imported -> imported
            status != RunnerResultStatus.New -> status
            else -> previousStatus ?: RunnerResultStatus.New
        }
        return if (decision == RunnerResultStatus.Restored) RunnerResultStatus.New else decision
    }
    val reviewVacancyId: Long? get() =
        if (status == RunnerResultStatus.Imported) vacancyId else previousVacancyId
}

/** Carries an untouched dataset row, including rows the current mapper cannot understand. */
data class RunnerRawResult(
    val position: Int,
    val source: String,
    val externalId: String?,
    val rawResultAsJson: String,
)
