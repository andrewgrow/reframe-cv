package reframecv.domain.models.importrecord

import reframecv.domain.DomainModel

/** Represents an original import snapshot and its source metadata. */
data class ImportRecord(
    val id: Long = 0,
    val provider: String,
    val rawData: String,
    val createdAt: Long,
    val externalId: String? = null,
    val sourceUrl: String? = null,
    val metadataJson: String? = null,
) : DomainModel
