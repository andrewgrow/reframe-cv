package reframecv.domain.models.vacancy

import reframecv.domain.DomainModel

/** Represents a vacancy in a project workspace. */
data class Vacancy(
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val description: String = "",
    val company: String = "",
    val url: String = "",
    val resumes: List<VacancyResume> = emptyList(),
    val coverLetterIds: List<Long> = emptyList(),
    val keywords: List<String> = emptyList(),
    val vacancyAttachmentId: Long? = null,
    val responsibilities: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val niceToHave: List<String> = emptyList(),
    val jobTitle: String = "",
    val appliedAt: Long? = null,
) : DomainModel
