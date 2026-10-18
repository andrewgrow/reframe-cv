package reframecv.providers.recognition

import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.RecognizedVacancy

/** Converts provider responses into the application recognition contract. */
fun VacancyRecognitionResult.toOutcome(): RecognitionOutcome {
    val result = RecognizedVacancy(
        jobTitle = jobTitle?.trim().orEmpty(),
        description = jobDescription?.trim().orEmpty(),
        company = company?.trim().orEmpty(),
        url = url?.trim().orEmpty(),
        keywords = keywords.cleaned().distinctBy { it.lowercase() },
        responsibilities = responsibilities.cleaned(),
        requirements = requirements.cleaned(),
        niceToHave = niceToHave.cleaned(),
    )
    val hasText = result.jobTitle.isNotBlank() || result.description.isNotBlank()
    return if (!hasText && result.responsibilities.isEmpty() && result.requirements.isEmpty()) {
        RecognitionOutcome.Failed(RecognitionError.NotRecognized)
    } else {
        RecognitionOutcome.Recognized(result)
    }
}

private fun List<String?>.cleaned() = filterNotNull().map(String::trim)
    .filter(String::isNotEmpty).distinct()
