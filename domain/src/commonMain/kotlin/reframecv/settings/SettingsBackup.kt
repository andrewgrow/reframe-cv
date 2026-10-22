package reframecv.settings

import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.settings.AppSettings
import reframecv.export.DocumentExportResult

/** Carries portable preferences independently of database identities and cached API responses. */
data class SettingsBackup(
    val settings: AppSettings? = null,
    val connections: List<ProviderConnection> = emptyList(),
    val prompts: List<Prompt> = emptyList(),
    val skipped: Int = 0,
    val runners: List<ConnectionRunner> = emptyList(),
) {
    override fun toString() =
        "SettingsBackup(connections=${connections.size}, prompts=${prompts.size})"
}

/** Summarizes restored settings without exposing credentials or prompt contents. */
data class SettingsImportReport(
    val connections: Int,
    val prompts: Int,
    val duplicates: Int = 0,
    val skipped: Int = 0,
    val windowRestored: Boolean = false,
)

/** Reads a consistent backup and restores supported records in one local transaction. */
interface SettingsBackupRepository {
    suspend fun snapshot(): SettingsBackup
    suspend fun restore(backup: SettingsBackup): SettingsImportReport
}

/** Provides native JSON selection and saving without exposing platform paths to settings. */
interface SettingsBackupFiles {
    suspend fun read(): String?
    suspend fun write(json: String): DocumentExportResult
}

/** Reports an unsupported or unreadable backup without retaining potentially secret JSON. */
class SettingsBackupFormatException : Exception("Invalid or unsupported settings backup")
