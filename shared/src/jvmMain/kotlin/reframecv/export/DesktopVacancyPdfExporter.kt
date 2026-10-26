package reframecv.export

import java.nio.file.Path
import java.time.Clock
import reframecv.documents.vacancy.DesktopVacancyPdfRenderer
import reframecv.documents.vacancy.VacancyPrintData
import reframecv.domain.models.vacancy.Vacancy

/** Generates printable vacancy content using the shared temporary-document lifecycle. */
class DesktopVacancyPdfExporter(
    picker: ExportDestinationPicker = DesktopExportDestinationPicker("Save vacancy PDF"),
    clock: Clock = Clock.systemDefaultZone(),
    temporaryDirectory: Path? = null,
    private val render: (
        Vacancy,
    ) -> ByteArray = { DesktopVacancyPdfRenderer.render(it.toPrintData()) },
) : VacancyExporter {
    private val files = DesktopDocumentExport(picker, clock, temporaryDirectory)
    override suspend fun export(vacancy: Vacancy): DocumentExportResult =
        files.export("vacancy", vacancy.id, "pdf") { render(vacancy) }
}

internal fun Vacancy.toPrintData() = VacancyPrintData(
    title = jobTitle.ifBlank { name },
    company = company,
    url = url,
    description = description,
    responsibilities = responsibilities,
    requirements = requirements,
    niceToHave = niceToHave,
)
