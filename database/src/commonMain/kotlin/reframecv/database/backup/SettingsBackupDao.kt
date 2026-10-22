package reframecv.database.backup

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import reframecv.database.prompts.PromptEntity
import reframecv.database.providers.ConnectionRunnerEntity
import reframecv.database.providers.ProviderConnectionEntity
import reframecv.database.providers.toEntity
import reframecv.database.settings.AppSettingsEntity
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.InvalidProviderConfiguration
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConfigurationJson
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

    @Query("SELECT * FROM connection_runners ORDER BY id")
    abstract suspend fun runners(): List<ConnectionRunnerEntity>

    @Insert
    abstract suspend fun insertRunner(runner: ConnectionRunnerEntity): Long

    @Insert
    abstract suspend fun insertConnection(connection: ProviderConnectionEntity): Long

    @Insert
    abstract suspend fun insertPrompt(prompt: PromptEntity): Long

    @Upsert
    abstract suspend fun saveSettings(settings: AppSettingsEntity)

    @Transaction
    open suspend fun snapshot() = SettingsBackup(
        settings()?.toDomainModel() ?: AppSettings(),
        connections().map { it.copy(cachedInfo = null).toDomainModel() },
        prompts().map { it.toDomainModel() },
        runners = runners().map { it.toDomainModel() },
    )

    @Transaction
    open suspend fun restore(backup: SettingsBackup): SettingsImportReport {
        val existingConnections = connections().toMutableList()
        val existingPrompts = prompts().toMutableList()
        val now = nowMillis()
        val connectionIds = mutableMapOf<Long, Long>()
        var addedConnections = 0
        var addedPrompts = 0
        backup.connections.forEach {
            val row = it.backupEntity(now)
            val existing = existingConnections.find { existing -> sameConnection(existing, row) }
            val id = existing?.id ?: insertConnection(row).also { id ->
                existingConnections += row.copy(id = id)
                addedConnections++
            }
            connectionIds[it.id] = id
        }
        backup.prompts.forEach {
            val row = it.backupEntity(now)
            if (existingPrompts.none { existing -> samePrompt(existing, row) }) {
                existingPrompts += row.copy(id = insertPrompt(row))
                addedPrompts++
            }
        }
        restoreRunners(backup, connectionIds)
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
    private suspend fun restoreRunners(backup: SettingsBackup, ids: Map<Long, Long>) {
        val existing = runners().toMutableList()
        val now = nowMillis()
        backup.runners.forEach { runner ->
            val owner = ids[runner.connectionId] ?: return@forEach
            val row = runner.copy(
                id = 0,
                connectionId = owner,
                createdAt = now,
                updatedAt = now,
            ).toEntity()
            if (existing.none { it.copy(id = 0, createdAt = now, updatedAt = now) == row }) {
                existing += row.copy(id = insertRunner(row))
            }
        }
    }
}

private fun sameConnection(a: ProviderConnectionEntity, b: ProviderConnectionEntity): Boolean {
    val first = a.toDomainModel()
    val second = b.toDomainModel()
    val redacted = ProviderConfigurationJson.portable(first.configuration, false)
    val incoming = ProviderConfigurationJson.portable(second.configuration, false)
    val sameIdentity = a.type == b.type && a.name == b.name && a.active == b.active
    if (!sameIdentity || redacted != incoming) return false
    return when (val config = second.configuration) {
        is OpenRouterConfiguration ->
            (config.apiKey.isEmpty() || config.apiKey == first.openRouter.apiKey) &&
                (
                    config.managementApiKey.isEmpty() ||
                        config.managementApiKey == first.openRouter.managementApiKey
                    )

        is ApifyConfiguration -> config.apiToken.isEmpty() ||
            config.apiToken == (first.configuration as? ApifyConfiguration)?.apiToken

        is InvalidProviderConfiguration -> first.configuration == second.configuration
    }
}

private fun samePrompt(a: PromptEntity, b: PromptEntity) =
    a.title == b.title && a.component == b.component && a.text == b.text && a.active == b.active

private fun ProviderConnection.backupEntity(now: Long) =
    copy(id = 0, createdAt = now, updatedAt = now, cachedInfo = null).toEntity()

private fun Prompt.backupEntity(now: Long) = PromptEntity(
    title = title,
    component = component?.name,
    text = text,
    active = active,
    createdAt = now,
    updatedAt = now,
)
