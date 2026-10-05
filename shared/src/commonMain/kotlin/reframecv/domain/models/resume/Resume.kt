package reframecv.domain.models.resume

import reframecv.domain.DomainModel

/** Resume timestamps are milliseconds since the Unix epoch. */
data class Resume(
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val content: String = "",
    val sourceResumeId: Long? = null,
    val keywords: List<String> = emptyList(),
    val deletedAt: Long? = null,
) : DomainModel
