package reframecv.ui.components.projects.vacancies.editor

import reframecv.attachments.SnapshotChange
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume

/** Keeps unfinished vacancy edits separate from the saved record. */
data class VacancyDraft(
    val name: String,
    val company: String = "",
    val description: String = "",
    val url: String = "",
    val keywords: String = "",
    val snapshotChange: SnapshotChange = SnapshotChange.Keep,
    val responsibilities: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val niceToHave: List<String> = emptyList(),
    val jobTitle: String = "",
    val resumes: List<VacancyResume> = emptyList(),
    val coverLetterIds: List<Long> = emptyList(),
) {
    fun toVacancy(projectId: Long) = applyTo(
        Vacancy(projectId = projectId, name = "", createdAt = 0, updatedAt = 0),
    )

    fun applyTo(vacancy: Vacancy) = vacancy.copy(
        resumes = resumes,
        coverLetterIds = coverLetterIds,
        name = name.trim(),
        jobTitle = jobTitle.trim(),
        company = company.trim(),
        description = description.trim(),
        url = url.trim(),
        responsibilities = responsibilities.map { it.trim() }.filter { it.isNotEmpty() },
        requirements = requirements.map { it.trim() }.filter { it.isNotEmpty() },
        niceToHave = niceToHave.map { it.trim() }.filter { it.isNotEmpty() },
        keywords = keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    )

    companion object {
        fun fromVacancy(vacancy: Vacancy) = VacancyDraft(
            resumes = vacancy.resumes,
            coverLetterIds = vacancy.coverLetterIds,
            name = vacancy.name,
            jobTitle = vacancy.jobTitle,
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
