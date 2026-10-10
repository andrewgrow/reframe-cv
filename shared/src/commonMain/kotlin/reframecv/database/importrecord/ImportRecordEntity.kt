package reframecv.database.importrecord

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.database.DatabaseModelFactory
import reframecv.domain.models.importrecord.ImportRecord

@Entity(
    tableName = "import_records",
    indices = [Index(value = ["provider", "external_id"])],
)
data class ImportRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val provider: String,
    @ColumnInfo(name = "raw_data")
    val rawData: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "external_id")
    val externalId: String? = null,
    @ColumnInfo(name = "source_url")
    val sourceUrl: String? = null,
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String? = null,
) : DatabaseModel<ImportRecord> {
    override fun toDomainModel() = ImportRecord(
        id = id,
        provider = provider,
        rawData = rawData,
        createdAt = createdAt,
        externalId = externalId,
        sourceUrl = sourceUrl,
        metadataJson = metadataJson,
    )

    companion object : DatabaseModelFactory<ImportRecord, ImportRecordEntity> {
        override fun fromDomainModel(domainModel: ImportRecord) = ImportRecordEntity(
            id = domainModel.id,
            provider = domainModel.provider,
            rawData = domainModel.rawData,
            createdAt = domainModel.createdAt,
            externalId = domainModel.externalId,
            sourceUrl = domainModel.sourceUrl,
            metadataJson = domainModel.metadataJson,
        )
    }
}
