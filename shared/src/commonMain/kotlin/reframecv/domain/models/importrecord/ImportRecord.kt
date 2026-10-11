package reframecv.domain.models.importrecord

import reframecv.domain.DomainModel

/** Represents an original import snapshot and its source metadata. */
data class ImportRecord(
    val id: Long = 0,
    val provider: String,
    val rawData: String? = null,
    val createdAt: Long,
    val externalId: String? = null,
    val sourceUrl: String? = null,
    val metadataJson: String? = null,
    val filePath: String? = null,
    val fileName: String? = null,
    val mediaType: String? = null,
    val fileSize: Long? = null,
    val sha256: String? = null,
) : DomainModel
