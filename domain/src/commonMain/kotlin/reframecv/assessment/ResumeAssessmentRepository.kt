package reframecv.assessment

import reframecv.domain.models.providers.ProviderConnection

/** Returns validated matching feedback or a safe provider failure. */
sealed interface ResumeAssessmentOutcome {
    data class Assessed(val result: ResumeAssessmentResult) : ResumeAssessmentOutcome
    data class Failed(val message: String) : ResumeAssessmentOutcome
}

/** Assesses two portable JSON documents without depending on networking or local storage. */
fun interface ResumeAssessmentRepository {
    suspend fun assess(
        resumeJson: String,
        vacancyJson: String,
        connection: ProviderConnection,
        instruction: String,
    ): ResumeAssessmentOutcome
}
