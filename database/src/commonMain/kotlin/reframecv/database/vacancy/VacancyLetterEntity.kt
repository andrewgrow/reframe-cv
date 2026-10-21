package reframecv.database.vacancy

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import reframecv.database.coverletter.CoverLetterEntity

/** Stores ordered letter links independently of the documents themselves. */
@Entity(
    tableName = "vacancies_letters",
    primaryKeys = ["vacancy_id", "letter_id"],
    foreignKeys = [
        ForeignKey(
            entity = VacancyEntity::class,
            parentColumns = ["id"],
            childColumns = ["vacancy_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CoverLetterEntity::class,
            parentColumns = ["id"],
            childColumns = ["letter_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["letter_id"])],
)
data class VacancyLetterEntity(
    @ColumnInfo(name = "vacancy_id") val vacancyId: Long,
    @ColumnInfo(name = "letter_id") val letterId: Long,
    val position: Int,
)
