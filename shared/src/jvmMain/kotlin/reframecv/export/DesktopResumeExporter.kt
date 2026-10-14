package reframecv.export

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Clock
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import reframecv.database.resume.ResumeContentJson
import reframecv.domain.models.resume.Resume

/** Generates a schema-compatible JSON file and removes its temporary copy after every outcome. */
class DesktopResumeExporter(
    private val picker: ExportDestinationPicker = DesktopExportDestinationPicker(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val temporaryDirectory: Path? = null,
) : ResumeExporter {
    override suspend fun export(resume: Resume): ResumeExportResult {
        require(resume.id > 0)
        var temporary: Path? = null
        try {
            val filename = withContext(Dispatchers.IO) {
                val stamp = DateTimeFormatter.ofPattern("yyyyddMM-HHmm-ss")
                    .withZone(clock.zone).format(clock.instant())
                temporary = if (temporaryDirectory == null) {
                    Files.createTempFile("reframecv-resume-", ".json")
                } else {
                    Files.createTempFile(temporaryDirectory, "reframecv-resume-", ".json")
                }
                Files.writeString(
                    checkNotNull(temporary),
                    ResumeContentJson.encodeForExport(resume.content),
                )
                "resume-${resume.id}-$stamp.json"
            }
            val destination = picker.pick(filename) ?: return ResumeExportResult.Cancelled
            withContext(Dispatchers.IO) {
                Files.copy(
                    checkNotNull(temporary),
                    Path.of(destination),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
            return ResumeExportResult.Saved
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                temporary?.let { Files.deleteIfExists(it) }
            }
        }
    }
}
