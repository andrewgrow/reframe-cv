package reframecv.database.vacancyattachment

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.database.DatabaseModelFactory
import reframecv.domain.models.vacancyattachment.VacancyAttachment

/** Represents a vacancy attachment and its file metadata in the local database. */
@Entity(tableName = "vacancy_attachments")
data class VacancyAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "media_type")
    val mediaType: String,
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    val sha256: String,
    @ColumnInfo(name = "source_url")
    val sourceUrl: String? = null,
) : DatabaseModel<VacancyAttachment> {
    override fun toDomainModel() = VacancyAttachment(
        id = id,
        createdAt = createdAt,
        sourceUrl = sourceUrl,
        filePath = filePath,
        fileName = fileName,
        mediaType = mediaType,
        fileSize = fileSize,
        sha256 = sha256,
    )

    companion object : DatabaseModelFactory<VacancyAttachment, VacancyAttachmentEntity> {
        override fun fromDomainModel(domainModel: VacancyAttachment) = VacancyAttachmentEntity(
            id = domainModel.id,
            createdAt = domainModel.createdAt,
            sourceUrl = domainModel.sourceUrl,
            filePath = domainModel.filePath,
            fileName = domainModel.fileName,
            mediaType = domainModel.mediaType,
            fileSize = domainModel.fileSize,
            sha256 = domainModel.sha256,
        )
    }
}
