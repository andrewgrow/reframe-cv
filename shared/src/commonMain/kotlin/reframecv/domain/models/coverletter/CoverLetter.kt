package reframecv.domain.models.coverletter

import reframecv.domain.DomainModel

/** Represents a cover letter in a project workspace. */
data class CoverLetter(
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val content: String = "",
    val keywords: List<String> = emptyList(),
) : DomainModel
