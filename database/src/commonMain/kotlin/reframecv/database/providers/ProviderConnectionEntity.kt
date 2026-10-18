package reframecv.database.providers

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.json.Json
import reframecv.database.DatabaseModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConnection

/** Represents a saved API connection in the local database. */
@Entity(tableName = "provider_connections")
data class ProviderConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val name: String,
    @ColumnInfo(name = "api_key") val apiKey: String,
    @ColumnInfo(name = "model_id") val modelId: String,
    val active: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "account_info") val accountInfo: String? = null,
) : DatabaseModel<ProviderConnection> {
    override fun toDomainModel() = ProviderConnection(
        id,
        ApiProviderType.valueOf(type),
        name,
        apiKey,
        modelId,
        active,
        createdAt,
        updatedAt,
        accountInfo?.let { Json.decodeFromString<ProviderAccountInfo>(it) },
    )
    override fun toString() = "ProviderConnectionEntity(id=$id, type=$type)"
}
