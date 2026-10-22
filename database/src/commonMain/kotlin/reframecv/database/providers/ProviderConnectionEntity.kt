package reframecv.database.providers

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConfigurationJson
import reframecv.domain.models.providers.ProviderConnection

/** Stores common connection fields and provider-owned JSON documents. */
@Entity(tableName = "provider_connections")
data class ProviderConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val name: String,
    val configuration: String,
    val active: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "cached_info") val cachedInfo: String? = null,
) : DatabaseModel<ProviderConnection> {
    override fun toDomainModel(): ProviderConnection {
        val provider = ApiProviderType.valueOf(type)
        return ProviderConnection(
            id,
            provider,
            name,
            ProviderConfigurationJson.decode(provider, configuration),
            active,
            createdAt,
            updatedAt,
            ProviderConfigurationJson.decodeCache(provider, cachedInfo),
        )
    }
    override fun toString() = "ProviderConnectionEntity(id=$id, type=$type)"
}

fun ProviderConnection.toEntity() = ProviderConnectionEntity(
    id,
    type.name,
    name,
    ProviderConfigurationJson.encode(configuration),
    active,
    createdAt,
    updatedAt,
    ProviderConfigurationJson.encodeCache(cachedInfo),
)
