package reframecv.export

import java.nio.file.Path
import java.time.Clock
import reframecv.database.resume.ResumeContentJson
import reframecv.domain.models.resume.Resume

/** Exports resume content through the desktop JSON file lifecycle. */
class DesktopResumeExporter(
    picker: ExportDestinationPicker = DesktopExportDestinationPicker(),
    clock: Clock = Clock.systemDefaultZone(),
    temporaryDirectory: Path? = null,
) : ResumeExporter {
    private val files = DesktopJsonExport(picker, clock, temporaryDirectory)

    override suspend fun export(resume: Resume): ResumeExportResult =
        files.export("resume", resume.id) { ResumeContentJson.encodeForExport(resume.content) }
}
