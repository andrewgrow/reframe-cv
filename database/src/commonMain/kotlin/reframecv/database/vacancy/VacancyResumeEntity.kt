package reframecv.database.vacancy

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import reframecv.database.resume.ResumeEntity
import reframecv.domain.models.vacancy.VacancyResume

/** Stores one vacancy–resume link and its ATS score independently of other vacancies. */
@Entity(
    tableName = "vacancies_resumes",
    primaryKeys = ["vacancy_id", "resume_id"],
    foreignKeys = [
        ForeignKey(
            entity = VacancyEntity::class,
            parentColumns = ["id"],
            childColumns = ["vacancy_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ResumeEntity::class,
            parentColumns = ["id"],
            childColumns = ["resume_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["resume_id"])],
)
data class VacancyResumeEntity(
    @ColumnInfo(name = "vacancy_id") val vacancyId: Long,
    @ColumnInfo(name = "resume_id") val resumeId: Long,
    @ColumnInfo(name = "ats_score") val atsScore: Int = 0,
    val position: Int = 0,
) {
    fun toDomainModel() = VacancyResume(resumeId, atsScore)
}
