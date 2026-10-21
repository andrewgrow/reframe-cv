package reframecv.export

import java.nio.file.Path
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import reframecv.documents.resume.DesktopResumePdfRenderer
import reframecv.domain.models.resume.Resume
import reframecv.photos.ResumePhotos

/** Reads only an included portrait and exports filtered resume content through the shared file lifecycle. */
class DesktopResumePdfExporter(
    private val photos: ResumePhotos,
    picker: ExportDestinationPicker = DesktopExportDestinationPicker("Save resume PDF"),
    clock: Clock = Clock.systemDefaultZone(),
    temporaryDirectory: Path? = null,
) : ResumeExporter {
    private val files = DesktopDocumentExport(picker, clock, temporaryDirectory)

    override suspend fun export(resume: Resume): ResumeExportResult {
        val photo = if (resume.photoVisible) resume.photoPath?.let { photos.read(it) } else null
        return withContext(Dispatchers.IO) {
            files.export("resume", resume.id, "pdf") {
                DesktopResumePdfRenderer.render(resume.toPrintData(photo))
            }
        }
    }
}
