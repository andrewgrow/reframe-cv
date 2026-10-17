package reframecv.ui.components.projects.vacancies.editor

import reframecv.attachments.SnapshotChange
import reframecv.domain.models.vacancy.Vacancy

/** Keeps unfinished vacancy edits separate from the saved record. */
data class VacancyDraft(
    val title: String,
    val company: String = "",
    val description: String = "",
    val url: String = "",
    val keywords: String = "",
    val snapshotChange: SnapshotChange = SnapshotChange.Keep,
    val responsibilities: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val niceToHave: List<String> = emptyList(),
) {
    fun toVacancy(projectId: Long) = applyTo(
        Vacancy(projectId = projectId, name = "", createdAt = 0, updatedAt = 0),
    )

    fun applyTo(vacancy: Vacancy) = vacancy.copy(
        name = title.trim(),
        company = company.trim(),
        description = description.trim(),
        url = url.trim(),
        responsibilities = responsibilities,
        requirements = requirements,
        niceToHave = niceToHave,
        keywords = keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    )

    companion object {
        fun fromVacancy(vacancy: Vacancy) = VacancyDraft(
            title = vacancy.name,
            company = vacancy.company,
            description = vacancy.description,
            url = vacancy.url,
            keywords = vacancy.keywords.joinToString(", "),
            responsibilities = vacancy.responsibilities,
            requirements = vacancy.requirements,
            niceToHave = vacancy.niceToHave,
        )
    }
}
