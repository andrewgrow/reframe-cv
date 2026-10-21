package reframecv.export

import java.nio.file.Path
import java.time.Clock
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.serialization.CoverLetterContentJson

/** Exports cover-letter content through the desktop JSON file lifecycle. */
class DesktopCoverLetterExporter(
    picker: ExportDestinationPicker = DesktopExportDestinationPicker("Export cover letter"),
    clock: Clock = Clock.systemDefaultZone(),
    temporaryDirectory: Path? = null,
) : CoverLetterExporter {
    private val files = DesktopJsonExport(picker, clock, temporaryDirectory)

    override suspend fun export(coverLetter: CoverLetter): DocumentExportResult =
        files.export("cover-letter", coverLetter.id) { CoverLetterContentJson.encode(coverLetter) }
}
