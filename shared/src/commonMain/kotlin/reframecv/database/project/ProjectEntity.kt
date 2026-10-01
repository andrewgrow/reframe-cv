package reframecv.database.project

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant
import reframecv.database.DatabaseModel
import reframecv.database.DatabaseModelFactory
import reframecv.domain.models.project.Project

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAtEpochMilliseconds: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMilliseconds: Long,
) : DatabaseModel<Project> {
    override fun toDomainModel() = Project(
        id = id,
        name = name,
        createdAt = Instant.fromEpochMilliseconds(createdAtEpochMilliseconds),
        updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMilliseconds),
    )

    companion object : DatabaseModelFactory<Project, ProjectEntity> {
        override fun fromDomainModel(domainModel: Project) = ProjectEntity(
            id = domainModel.id,
            name = domainModel.name,
            createdAtEpochMilliseconds = domainModel.createdAt.toEpochMilliseconds(),
            updatedAtEpochMilliseconds = domainModel.updatedAt.toEpochMilliseconds(),
        )
    }
}
