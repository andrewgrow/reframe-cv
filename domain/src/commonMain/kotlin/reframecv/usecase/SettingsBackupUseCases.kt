package reframecv.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import reframecv.export.DocumentExportResult
import reframecv.settings.SettingsBackupFiles
import reframecv.settings.SettingsBackupJson
import reframecv.settings.SettingsBackupRepository

/** Coordinates portable settings backups without depending on the database or native file dialogs. */
class SettingsBackupUseCases(
    private val repository: SettingsBackupRepository,
    private val files: SettingsBackupFiles,
) {
    suspend fun export(includeApiKeys: Boolean): DocumentExportResult {
        val snapshot = repository.snapshot()
        val json =
            withContext(Dispatchers.Default) { SettingsBackupJson.encode(snapshot, includeApiKeys) }
        return files.write(json)
    }

    suspend fun importSettings() = files.read()?.let { source ->
        val backup = withContext(Dispatchers.Default) { SettingsBackupJson.decode(source) }
        repository.restore(backup)
    }
}
