package reframecv.ui.components.projects.vacancies.editor

import reframecv.domain.models.vacancy.Vacancy

data class VacancyDraft(
    val title: String,
    val company: String = "",
    val description: String = "",
    val url: String = "",
    val keywords: String = "",
) {
    fun toVacancy(projectId: Long) = applyTo(
        Vacancy(projectId = projectId, name = "", createdAt = 0, updatedAt = 0),
    )

    fun applyTo(vacancy: Vacancy) = vacancy.copy(
        name = title.trim(),
        company = company.trim(),
        description = description.trim(),
        url = url.trim(),
        keywords = keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    )

    companion object {
        fun fromVacancy(vacancy: Vacancy) = VacancyDraft(
            title = vacancy.name,
            company = vacancy.company,
            description = vacancy.description,
            url = vacancy.url,
            keywords = vacancy.keywords.joinToString(", "),
        )
    }
}
