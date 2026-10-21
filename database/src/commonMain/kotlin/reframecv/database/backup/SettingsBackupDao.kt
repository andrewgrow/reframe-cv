package reframecv.database.backup

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import reframecv.database.prompts.PromptEntity
import reframecv.database.providers.ProviderConnectionEntity
import reframecv.database.settings.AppSettingsEntity
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.settings.AppSettings
import reframecv.settings.SettingsBackup
import reframecv.settings.SettingsImportReport
import reframecv.shared.nowMillis

/** Captures and restores settings atomically while preserving existing records and credentials. */
@Dao
abstract class SettingsBackupDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    abstract suspend fun settings(): AppSettingsEntity?

    @Query("SELECT * FROM provider_connections ORDER BY id ASC")
    abstract suspend fun connections(): List<ProviderConnectionEntity>

    @Query("SELECT * FROM prompts ORDER BY id ASC")
    abstract suspend fun prompts(): List<PromptEntity>

    @Insert
    abstract suspend fun insertConnection(connection: ProviderConnectionEntity): Long

    @Insert
    abstract suspend fun insertPrompt(prompt: PromptEntity): Long

    @Upsert
    abstract suspend fun saveSettings(settings: AppSettingsEntity)

    @Transaction
    open suspend fun snapshot() = SettingsBackup(
        settings()?.toDomainModel() ?: AppSettings(),
        connections().map { it.copy(accountInfo = null, creditBalance = null).toDomainModel() },
        prompts().map { it.toDomainModel() },
    )

    @Transaction
    open suspend fun restore(backup: SettingsBackup): SettingsImportReport {
        val existingConnections = connections().toMutableList()
        val existingPrompts = prompts().toMutableList()
        val now = nowMillis()
        var addedConnections = 0
        var addedPrompts = 0
        backup.connections.forEach {
            val row = it.backupEntity(now)
            if (existingConnections.none { existing -> sameConnection(existing, row) }) {
                existingConnections += row.copy(id = insertConnection(row))
                addedConnections++
            }
        }
        backup.prompts.forEach {
            val row = it.backupEntity(now)
            if (existingPrompts.none { existing -> samePrompt(existing, row) }) {
                existingPrompts += row.copy(id = insertPrompt(row))
                addedPrompts++
            }
        }
        backup.settings?.window?.let {
            saveSettings(
                AppSettingsEntity(
                    windowWidth = it.width,
                    windowHeight = it.height,
                    windowMaximized = it.maximized,
                ),
            )
        }
        return SettingsImportReport(
            addedConnections,
            addedPrompts,
            backup.connections.size + backup.prompts.size - addedConnections - addedPrompts,
            backup.skipped,
            backup.settings != null,
        )
    }
}

private fun sameConnection(a: ProviderConnectionEntity, b: ProviderConnectionEntity) =
    a.type == b.type && a.name == b.name && a.modelId == b.modelId && a.active == b.active &&
        (b.apiKey.isEmpty() || a.apiKey == b.apiKey) &&
        (b.managementApiKey.isEmpty() || a.managementApiKey == b.managementApiKey)

private fun samePrompt(a: PromptEntity, b: PromptEntity) =
    a.title == b.title && a.component == b.component && a.text == b.text && a.active == b.active

private fun ProviderConnection.backupEntity(now: Long) = ProviderConnectionEntity(
    type = type.name,
    name = name,
    apiKey = apiKey,
    modelId = modelId,
    active = active,
    createdAt = now,
    updatedAt = now,
    managementApiKey = managementApiKey,
)

private fun Prompt.backupEntity(now: Long) = PromptEntity(
    title = title,
    component = component?.name,
    text = text,
    active = active,
    createdAt = now,
    updatedAt = now,
)
