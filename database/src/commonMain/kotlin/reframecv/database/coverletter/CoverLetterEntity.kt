package reframecv.database.coverletter

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.database.DatabaseModelFactory
import reframecv.database.project.ProjectEntity
import reframecv.database.resume.ResumeEntity
import reframecv.domain.models.coverletter.CoverLetter

/** Represents a cover letter record in the local database. */
@Entity(
    tableName = "cover_letters",
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
data class CoverLetterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "project_id")
    val projectId: Long,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    val content: String = "",
    @ColumnInfo(name = "source_resume_id") val sourceResumeId: Long? = null,
) : DatabaseModel<CoverLetter> {
    override fun toDomainModel() = CoverLetter(
        id = id,
        projectId = projectId,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        content = content,
        sourceResumeId = sourceResumeId,
    )

    companion object : DatabaseModelFactory<CoverLetter, CoverLetterEntity> {
        override fun fromDomainModel(domainModel: CoverLetter) = CoverLetterEntity(
            id = domainModel.id,
            projectId = domainModel.projectId,
            name = domainModel.name,
            createdAt = domainModel.createdAt,
            updatedAt = domainModel.updatedAt,
            content = domainModel.content,
            sourceResumeId = domainModel.sourceResumeId,
        )
    }
}
