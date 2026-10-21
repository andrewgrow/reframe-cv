package reframecv.coverletters

import reframecv.domain.models.providers.ProviderConnection

/** Generates letter text from portable resume and vacancy documents. */
fun interface CoverLetterGenerationRepository {
    suspend fun generate(
        resumeJson: String,
        vacancyJson: String,
        connection: ProviderConnection,
        instruction: String,
    ): CoverLetterGenerationOutcome
}

/** Keeps invalid provider output separate from usable letter text. */
sealed interface CoverLetterGenerationOutcome {
    data class Generated(val content: String) : CoverLetterGenerationOutcome
    data class Failed(val message: String) : CoverLetterGenerationOutcome
}
