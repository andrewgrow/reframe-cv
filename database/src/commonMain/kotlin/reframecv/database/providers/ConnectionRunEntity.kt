package reframecv.database.providers

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunState

/** Stores execution history and the exact non-secret input used for each run. */
@Entity(
    tableName = "connection_runs",
    foreignKeys = [
        ForeignKey(
            entity = ConnectionRunnerEntity::class,
            parentColumns = ["id"],
            childColumns = ["runner_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("runner_id")],
)
data class ConnectionRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "runner_id") val runnerId: Long,
    @ColumnInfo(name = "configuration_snapshot") val configurationSnapshot: String,
    val state: String,
    @ColumnInfo(name = "external_id") val externalId: String?,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long?,
    val error: String?,
) : DatabaseModel<ConnectionRun> {
    override fun toDomainModel() = ConnectionRun(
        id,
        runnerId,
        configurationSnapshot,
        ConnectionRunState.valueOf(state),
        externalId,
        startedAt,
        finishedAt,
        error,
    )
}
fun ConnectionRun.toEntity() = ConnectionRunEntity(
    id,
    runnerId,
    configurationSnapshot,
    state.name,
    externalId,
    startedAt,
    finishedAt,
    error,
)
