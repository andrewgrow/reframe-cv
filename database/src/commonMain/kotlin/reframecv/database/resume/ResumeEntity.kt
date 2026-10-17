package reframecv.database.resume

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.database.DatabaseModelFactory
import reframecv.database.project.ProjectEntity
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.serialization.ResumeContentJson

/** Represents a resume record in the local database. */
@Entity(
    tableName = "resumes",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ResumeEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_resume_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["project_id"]), Index(value = ["source_resume_id"])],
)
data class ResumeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "project_id")
    val projectId: Long,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "content")
    val contentJson: String = ResumeContentJson.encode(ResumeContent()),
    @ColumnInfo(name = "source_resume_id")
    val sourceResumeId: Long? = null,
    @ColumnInfo(name = "photo_path")
    val photoPath: String? = null,
    @ColumnInfo(name = "photo_visible", defaultValue = "1")
    val photoVisible: Boolean = true,
) : DatabaseModel<Resume> {
    override fun toDomainModel() = Resume(
        id = id,
        projectId = projectId,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        content = ResumeContentJson.decode(contentJson),
        sourceResumeId = sourceResumeId,
        photoPath = photoPath,
        photoVisible = photoVisible,
    )

    companion object : DatabaseModelFactory<Resume, ResumeEntity> {
        override fun fromDomainModel(domainModel: Resume) = ResumeEntity(
            id = domainModel.id,
            projectId = domainModel.projectId,
            name = domainModel.name,
            createdAt = domainModel.createdAt,
            updatedAt = domainModel.updatedAt,
            contentJson = ResumeContentJson.encode(domainModel.content),
            sourceResumeId = domainModel.sourceResumeId,
            photoPath = domainModel.photoPath,
            photoVisible = domainModel.photoVisible,
        )
    }
}
