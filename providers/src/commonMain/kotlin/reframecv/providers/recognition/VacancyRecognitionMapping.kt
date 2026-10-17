package reframecv.providers.recognition

import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.RecognizedVacancy

/** Converts provider responses into the application recognition contract. */
fun VacancyRecognitionResult.toOutcome(): RecognitionOutcome = when (statusCode) {
    VacancyRecognitionResult.SUCCESS -> RecognitionOutcome.Recognized(
        RecognizedVacancy(
            jobTitle = jobTitle?.trim().orEmpty(),
            description = jobDescription?.trim().orEmpty(),
            company = company?.trim().orEmpty(),
            url = url?.trim().orEmpty(),
            keywords = keywords.cleaned().distinctBy { it.lowercase() },
            responsibilities = responsibilities.cleaned(),
            requirements = requirements.cleaned(),
            niceToHave = niceToHave.cleaned(),
        ),
    )

    VacancyRecognitionResult.NOT_RECOGNIZED ->
        RecognitionOutcome.Failed(RecognitionError.NotRecognized, statusDetails)

    else -> RecognitionOutcome.Failed(RecognitionError.ServiceUnavailable, statusDetails)
}

private fun List<String?>.cleaned() = filterNotNull().map(String::trim)
    .filter(String::isNotEmpty).distinct()
