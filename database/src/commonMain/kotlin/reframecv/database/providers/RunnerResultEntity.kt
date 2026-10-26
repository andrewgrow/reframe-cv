package reframecv.database.providers

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.database.vacancy.VacancyEntity
import reframecv.domain.models.providers.RunnerResult
import reframecv.domain.models.providers.RunnerResultStatus

/** Stores original runner output separately from editable vacancies and review decisions. */
@Entity(
    tableName = "runner_results",
    foreignKeys = [
        ForeignKey(
            entity = ConnectionRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["run_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = VacancyEntity::class,
            parentColumns = ["id"],
            childColumns = ["vacancy_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["run_id", "position"], unique = true),
        Index(value = ["run_id", "source", "external_id"], unique = true),
        Index(value = ["source", "external_id"]),
        Index(value = ["vacancy_id"]),
    ],
)
data class RunnerResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "run_id") val runId: Long,
    val position: Int,
    val source: String,
    @ColumnInfo(name = "external_id") val externalId: String?,
    @ColumnInfo(name = "raw_result_as_json") val rawResultAsJson: String,
    @ColumnInfo(name = "received_at") val receivedAt: Long,
    val status: String = RunnerResultStatus.New.name,
    @ColumnInfo(name = "vacancy_id") val vacancyId: Long? = null,
    @ColumnInfo(name = "relevance_score") val relevanceScore: Int? = null,
    @ColumnInfo(name = "relevance_prompt") val relevancePrompt: String? = null,
) : DatabaseModel<RunnerResult> {
    override fun toDomainModel() = RunnerResult(
        id, runId, position, source, externalId, rawResultAsJson, receivedAt,
        RunnerResultStatus.valueOf(status), vacancyId,
        relevanceScore = relevanceScore, relevancePrompt = relevancePrompt,
    )
}
