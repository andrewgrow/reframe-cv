package reframecv.domain.models.vacancyattachment

import reframecv.domain.DomainModel

/** Represents an immutable file attached as the original source of a vacancy. */
data class VacancyAttachment(
    val id: Long = 0,
    val createdAt: Long,
    val filePath: String,
    val fileName: String,
    val mediaType: String,
    val fileSize: Long,
    val sha256: String,
    val sourceUrl: String? = null,
) : DomainModel
