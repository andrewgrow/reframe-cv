package reframecv.database.vacancy

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.database.DatabaseModelFactory
import reframecv.database.coverletter.CoverLetterEntity
import reframecv.database.project.ProjectEntity
import reframecv.database.resume.ResumeEntity
import reframecv.domain.models.vacancy.Vacancy

@Entity(
    tableName = "vacancies",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ResumeEntity::class,
            parentColumns = ["id"],
            childColumns = ["resume_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CoverLetterEntity::class,
            parentColumns = ["id"],
            childColumns = ["cover_letter_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(
            value = ["project_id"],
        ), Index(value = ["resume_id"]), Index(value = ["cover_letter_id"]),
    ],
)
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
    @ColumnInfo(name = "resume_id")
    val resumeId: Long? = null,
    @ColumnInfo(name = "cover_letter_id")
    val coverLetterId: Long? = null,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
) : DatabaseModel<Vacancy> {
    override fun toDomainModel() = Vacancy(
        id = id,
        projectId = projectId,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        description = description,
        company = company,
        url = url,
        resumeId = resumeId,
        coverLetterId = coverLetterId,
        deletedAt = deletedAt,
    )

    companion object : DatabaseModelFactory<Vacancy, VacancyEntity> {
        override fun fromDomainModel(domainModel: Vacancy) = VacancyEntity(
            id = domainModel.id,
            projectId = domainModel.projectId,
            name = domainModel.name,
            createdAt = domainModel.createdAt,
            updatedAt = domainModel.updatedAt,
            description = domainModel.description,
            company = domainModel.company,
            url = domainModel.url,
            resumeId = domainModel.resumeId,
            coverLetterId = domainModel.coverLetterId,
            deletedAt = domainModel.deletedAt,
        )
    }
}
