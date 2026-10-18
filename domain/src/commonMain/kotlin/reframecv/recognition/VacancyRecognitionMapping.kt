package reframecv.recognition

import reframecv.domain.models.vacancy.Vacancy

fun RecognizedVacancy.applyTo(vacancy: Vacancy): Vacancy = vacancy.copy(
    jobTitle = jobTitle,
    company = company,
    url = url,
    description = description,
    keywords = keywords,
    responsibilities = responsibilities,
    requirements = requirements,
    niceToHave = niceToHave,
)
