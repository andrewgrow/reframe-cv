package reframecv.domain.models.importrecord

import reframecv.domain.DomainModel

/** An immutable source snapshot. Timestamps are milliseconds since the Unix epoch. */
data class ImportRecord(
    val id: Long = 0,
    val provider: String,
    val rawData: String,
    val createdAt: Long,
    val externalId: String? = null,
    val sourceUrl: String? = null,
    val metadataJson: String? = null,
) : DomainModel
