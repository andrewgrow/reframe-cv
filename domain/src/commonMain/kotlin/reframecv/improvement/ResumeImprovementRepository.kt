package reframecv.improvement

import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.resume.content.ResumeContent

/** Generates validated resume content from portable resume and vacancy documents. */
fun interface ResumeImprovementRepository {
    suspend fun improve(
        resumeJson: String,
        vacancyJson: String,
        connection: ProviderConnection,
        instruction: String,
    ): ResumeImprovementOutcome
}

/** Separates a generated adaptation from a displayable provider or contract failure. */
sealed interface ResumeImprovementOutcome {
    data class Improved(val content: ResumeContent) : ResumeImprovementOutcome
    data class Failed(val message: String) : ResumeImprovementOutcome
}
