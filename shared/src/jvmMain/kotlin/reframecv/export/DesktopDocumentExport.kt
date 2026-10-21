package reframecv.export

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Clock
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Owns generated documents until destination selection finishes, including errors and cancellation. */
internal class DesktopDocumentExport(
    private val picker: ExportDestinationPicker,
    private val clock: Clock,
    private val temporaryDirectory: Path?,
) {
    suspend fun export(
        kind: String,
        id: Long,
        extension: String,
        content: () -> ByteArray,
    ): DocumentExportResult {
        require(id > 0)
        var temporary: Path? = null
        try {
            val filename = withContext(Dispatchers.IO) {
                val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm-ss")
                    .withZone(clock.zone).format(clock.instant())
                temporary = if (temporaryDirectory == null) {
                    Files.createTempFile("reframecv-$kind-", ".$extension")
                } else {
                    Files.createTempFile(temporaryDirectory, "reframecv-$kind-", ".$extension")
                }
                Files.write(checkNotNull(temporary), content())
                "$kind-$id-$stamp.$extension"
            }
            val destination = picker.pick(filename) ?: return DocumentExportResult.Cancelled
            withContext(Dispatchers.IO) {
                Files.copy(
                    checkNotNull(temporary),
                    Path.of(destination),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
            return DocumentExportResult.Saved
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                temporary?.let { Files.deleteIfExists(it) }
            }
        }
    }
}
