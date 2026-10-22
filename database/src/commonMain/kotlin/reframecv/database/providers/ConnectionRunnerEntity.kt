package reframecv.database.providers

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.domain.models.providers.ConnectionOperation
import reframecv.domain.models.providers.ConnectionRunner

/** Stores reusable provider operations and removes them with their connection. */
@Entity(
    tableName = "connection_runners",
    foreignKeys = [
        ForeignKey(
            entity = ProviderConnectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["connection_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("connection_id")],
)
data class ConnectionRunnerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "connection_id") val connectionId: Long,
    val name: String,
    val operation: String,
    val configuration: String,
    val active: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) : DatabaseModel<ConnectionRunner> {
    override fun toDomainModel() = ConnectionRunner(
        id,
        connectionId,
        name,
        ConnectionOperation.valueOf(operation),
        configuration,
        active,
        createdAt,
        updatedAt,
    )
}
fun ConnectionRunner.toEntity() = ConnectionRunnerEntity(
    id,
    connectionId,
    name,
    operation.name,
    configuration,
    active,
    createdAt,
    updatedAt,
)
