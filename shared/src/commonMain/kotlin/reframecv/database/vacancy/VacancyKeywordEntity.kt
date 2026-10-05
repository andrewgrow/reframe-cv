package reframecv.database.vacancy

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "vacancies_keywords",
    primaryKeys = ["record_id", "normalized_keyword"],
    foreignKeys = [
        ForeignKey(
            entity = VacancyEntity::class,
            parentColumns = ["id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["normalized_keyword"])],
)
data class VacancyKeywordEntity(
    @ColumnInfo(name = "record_id")
    val recordId: Long,
    val keyword: String,
    @ColumnInfo(name = "normalized_keyword")
    val normalizedKeyword: String,
    val position: Int,
)
