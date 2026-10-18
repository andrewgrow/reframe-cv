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
                    responsibilities = listOf("Build apps, APIs", "Work with teams\nReview code"),
                    requirements = listOf("Kotlin"),
                    niceToHave = listOf("CI/CD"),
                ),
            ),
            response.toOutcome(),
        )
    }

    @Test
    fun emptyContentIsNotRecognizedEvenWhenCompanyOrUrlWasExtracted() {
        val response = VacancyRecognitionJson.decode(recognitionFixture()).copy(
            jobTitle = null,
            jobDescription = null,
            responsibilities = emptyList(),
            requirements = emptyList(),
            niceToHave = emptyList(),
        )
        assertEquals(
            RecognitionOutcome.Failed(RecognitionError.NotRecognized),
            response.toOutcome(),
        )
    }
}
