package reframecv.domain.models.vacancy

import reframecv.domain.DomainModel

/** Vacancy timestamps are milliseconds since the Unix epoch. */
data class Vacancy(
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val description: String = "",
    val company: String = "",
    val url: String = "",
    val resumeId: Long? = null,
    val coverLetterId: Long? = null,
    val keywords: List<String> = emptyList(),
    val importRecordId: Long? = null,
) : DomainModel
