package reframecv.export

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import reframecv.importing.DesktopJsonImportPicker
import reframecv.importing.JsonImportPicker
import reframecv.settings.SettingsBackupFiles

/** Reads bounded JSON sources and cleans up exported settings after saving or cancelling. */
class DesktopSettingsBackupFiles(
    private val source: JsonImportPicker = DesktopJsonImportPicker("Import settings"),
    destination: ExportDestinationPicker = DesktopExportDestinationPicker("Export settings"),
    clock: Clock = Clock.systemDefaultZone(),
    temporaryDirectory: Path? = null,
) : SettingsBackupFiles {
    private val files = DesktopDocumentExport(destination, clock, temporaryDirectory)
    override suspend fun read(): String? {
        val path = source.pick() ?: return null
        return withContext(Dispatchers.IO) {
            Files.newInputStream(Path.of(path)).use { stream ->
                val bytes = stream.readNBytes(MAX_BACKUP_BYTES + 1)
                require(bytes.size <= MAX_BACKUP_BYTES) { "Settings backup is too large" }
                bytes.decodeToString(throwOnInvalidSequence = true)
            }
        }
    }
    override suspend fun write(json: String) =
        files.export("settings", null, "json") { json.encodeToByteArray() }
}

private const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
