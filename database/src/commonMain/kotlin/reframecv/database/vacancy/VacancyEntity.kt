package reframecv.database.vacancy

import androidx.room3.ColumnInfo
import androidx.room3.ColumnTypeConverters
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.database.DatabaseModelFactory
import reframecv.database.project.ProjectEntity
import reframecv.database.vacancyattachment.VacancyAttachmentEntity
import reframecv.domain.models.vacancy.Vacancy

/** Represents a vacancy record in the local database. */
@Entity(
    tableName = "vacancies",
    foreignKeys = [
        ForeignKey(
            entity = VacancyAttachmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["vacancy_attachment_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["vacancy_attachment_id"]),
        Index(
            value = ["project_id"],
        ),
    ],
)
@ColumnTypeConverters(VacancyListConverter::class)
data class VacancyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "project_id")
    val projectId: Long,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    val description: String = "",
    val company: String = "",
    val url: String = "",
    @ColumnInfo(name = "vacancy_attachment_id")
    val vacancyAttachmentId: Long? = null,
    @ColumnInfo(name = "responsibilities", defaultValue = "'[]'")
    val responsibilities: List<String> = emptyList(),
    @ColumnInfo(name = "requirements", defaultValue = "'[]'")
    val requirements: List<String> = emptyList(),
    @ColumnInfo(name = "nice_to_have", defaultValue = "'[]'")
    val niceToHave: List<String> = emptyList(),
    @ColumnInfo(name = "job_title", defaultValue = "''")
    val jobTitle: String = "",
) : DatabaseModel<Vacancy> {
    override fun toDomainModel() = Vacancy(
        id = id,
        projectId = projectId,
        name = name,
        jobTitle = jobTitle,
        createdAt = createdAt,
        updatedAt = updatedAt,
        description = description,
        company = company,
        url = url,
        vacancyAttachmentId = vacancyAttachmentId,
        responsibilities = responsibilities,
        requirements = requirements,
        niceToHave = niceToHave,
    )

    companion object : DatabaseModelFactory<Vacancy, VacancyEntity> {
        override fun fromDomainModel(domainModel: Vacancy) = VacancyEntity(
            id = domainModel.id,
            projectId = domainModel.projectId,
            name = domainModel.name,
            jobTitle = domainModel.jobTitle,
            createdAt = domainModel.createdAt,
            updatedAt = domainModel.updatedAt,
            description = domainModel.description,
            company = domainModel.company,
            url = domainModel.url,
            vacancyAttachmentId = domainModel.vacancyAttachmentId,
            responsibilities = domainModel.responsibilities,
            requirements = domainModel.requirements,
            niceToHave = domainModel.niceToHave,
        )
    }
}
