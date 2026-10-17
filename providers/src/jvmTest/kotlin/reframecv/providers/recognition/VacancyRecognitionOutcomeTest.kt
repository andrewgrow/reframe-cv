package reframecv.providers.recognition

import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.RecognizedVacancy

class VacancyRecognitionOutcomeTest {
    @Test
    fun successfulResponseExposesOnlyNormalizedDomainContent() {
        val response = VacancyRecognitionJson.decode(recognitionFixture())
        assertEquals(
            RecognitionOutcome.Recognized(
                RecognizedVacancy(
                    jobTitle = "Mobile developer",
                    description = "Build mobile apps",
                    company = "Recognized company",
                    url = "https://example.com/recognized",
                    keywords = listOf("Kotlin", "Android"),
                    responsibilities = listOf("Build apps, APIs", "Work with teams\nReview code"),
                    requirements = listOf("Kotlin"),
                    niceToHave = listOf("CI/CD"),
                ),
            ),
            response.toOutcome(),
        )
    }

    @Test
    fun failedResponsesExposeTypedErrorsAndDetailsInsteadOfPostingContent() {
        val response = VacancyRecognitionJson.decode(recognitionFixture())
        assertEquals(
            RecognitionOutcome.Failed(RecognitionError.NotRecognized, "No job found"),
            response.copy(statusCode = 400, statusDetails = "No job found").toOutcome(),
        )
        assertEquals(
            RecognitionOutcome.Failed(RecognitionError.ServiceUnavailable, "Provider failure"),
            response.copy(statusCode = 500, statusDetails = "Provider failure").toOutcome(),
        )
    }
}
