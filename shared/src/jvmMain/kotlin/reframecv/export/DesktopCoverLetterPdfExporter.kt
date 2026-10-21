package reframecv.export

import java.nio.file.Path
import java.time.Clock
import reframecv.documents.coverletter.CoverLetterPrintData
import reframecv.documents.coverletter.DesktopCoverLetterPdfRenderer
import reframecv.domain.models.coverletter.CoverLetter

/** Generates printable letter text and owns its temporary PDF until saving finishes. */
class DesktopCoverLetterPdfExporter(
    picker: ExportDestinationPicker = DesktopExportDestinationPicker("Save cover letter PDF"),
    clock: Clock = Clock.systemDefaultZone(),
    temporaryDirectory: Path? = null,
    private val render: (CoverLetter) -> ByteArray = {
        DesktopCoverLetterPdfRenderer.render(CoverLetterPrintData(it.content))
    },
) : CoverLetterExporter {
    private val files = DesktopDocumentExport(picker, clock, temporaryDirectory)

    override suspend fun export(coverLetter: CoverLetter): DocumentExportResult =
        files.export("cover-letter", coverLetter.id, "pdf") { render(coverLetter) }
}
