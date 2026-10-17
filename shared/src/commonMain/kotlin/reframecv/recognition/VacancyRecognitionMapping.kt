package reframecv.recognition

import reframecv.domain.models.vacancy.Vacancy

fun VacancyRecognitionResult.applyTo(vacancy: Vacancy): Vacancy {
    require(statusCode == VacancyRecognitionResult.SUCCESS) {
        "Cannot apply unsuccessful vacancy recognition"
    }
    val title = jobTitle?.trim().orEmpty()
    return vacancy.copy(
        name = if (vacancy.name.isBlank()) title else vacancy.name,
        jobTitle = title,
        company = company?.trim().orEmpty(),
        url = url?.trim().orEmpty(),
        description = jobDescription?.trim().orEmpty(),
        keywords = keywords.cleaned().distinctBy { it.lowercase() },
        responsibilities = responsibilities.cleaned(),
        requirements = requirements.cleaned(),
        niceToHave = niceToHave.cleaned(),
    )
}

private fun List<String?>.cleaned() = filterNotNull().map(String::trim)
    .filter(String::isNotEmpty).distinct()
