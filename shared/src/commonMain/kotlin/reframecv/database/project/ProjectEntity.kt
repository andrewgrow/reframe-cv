package reframecv.database.project

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.database.DatabaseModelFactory
import reframecv.database.resume.ResumeEntity
import reframecv.domain.models.project.Project
import reframecv.domain.models.project.ProjectMode

/** Represents a project record in the local database. */
@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ResumeEntity::class,
            parentColumns = ["id"],
            childColumns = ["main_resume_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["parent_id"]), Index(value = ["main_resume_id"])],
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "parent_id")
    val parentId: Long? = null,
    @ColumnInfo(defaultValue = "'Unconfigured'")
    val mode: ProjectMode = ProjectMode.Unconfigured,
    @ColumnInfo(name = "main_resume_id")
    val mainResumeId: Long? = null,
) : DatabaseModel<Project> {
    override fun toDomainModel() = Project(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        parentId = parentId,
        mode = mode,
        mainResumeId = mainResumeId,
    )

    companion object : DatabaseModelFactory<Project, ProjectEntity> {
        override fun fromDomainModel(domainModel: Project) = ProjectEntity(
            id = domainModel.id,
            name = domainModel.name,
            createdAt = domainModel.createdAt,
            updatedAt = domainModel.updatedAt,
            parentId = domainModel.parentId,
            mode = domainModel.mode,
            mainResumeId = domainModel.mainResumeId,
        )
    }
}
