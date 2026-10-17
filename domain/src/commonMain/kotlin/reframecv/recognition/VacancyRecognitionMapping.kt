package reframecv.recognition

import reframecv.domain.models.vacancy.Vacancy

fun RecognizedVacancy.applyTo(vacancy: Vacancy): Vacancy = vacancy.copy(
    name = if (vacancy.name.isBlank()) jobTitle else vacancy.name,
    jobTitle = jobTitle,
    company = company,
    url = url,
    description = description,
    keywords = keywords,
    responsibilities = responsibilities,
    requirements = requirements,
    niceToHave = niceToHave,
)
