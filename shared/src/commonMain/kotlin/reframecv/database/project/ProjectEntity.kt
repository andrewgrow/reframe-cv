package reframecv.database.project

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.database.DatabaseModelFactory
import reframecv.domain.models.project.Project
import reframecv.domain.models.project.ProjectMode

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["parent_id"])],
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
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    @ColumnInfo(defaultValue = "'Unconfigured'")
    val mode: ProjectMode = ProjectMode.Unconfigured,
) : DatabaseModel<Project> {
    override fun toDomainModel() = Project(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        parentId = parentId,
        deletedAt = deletedAt,
        mode = mode,
    )

    companion object : DatabaseModelFactory<Project, ProjectEntity> {
        override fun fromDomainModel(domainModel: Project) = ProjectEntity(
            id = domainModel.id,
            name = domainModel.name,
            createdAt = domainModel.createdAt,
            updatedAt = domainModel.updatedAt,
            parentId = domainModel.parentId,
            deletedAt = domainModel.deletedAt,
            mode = domainModel.mode,
        )
    }
}
