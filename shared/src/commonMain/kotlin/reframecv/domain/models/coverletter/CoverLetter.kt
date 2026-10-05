package reframecv.domain.models.coverletter

import reframecv.domain.DomainModel

/** CoverLetter timestamps are milliseconds since the Unix epoch. */
data class CoverLetter(
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val content: String = "",
    val keywords: List<String> = emptyList(),
    val deletedAt: Long? = null,
) : DomainModel
