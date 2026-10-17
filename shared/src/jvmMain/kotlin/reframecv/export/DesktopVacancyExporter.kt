package reframecv.export

import java.nio.file.Path
import java.time.Clock
import reframecv.domain.models.vacancy.Vacancy

/** Exports vacancy content through the desktop JSON file lifecycle. */
class DesktopVacancyExporter(
    picker: ExportDestinationPicker = DesktopExportDestinationPicker("Export vacancy"),
    clock: Clock = Clock.systemDefaultZone(),
    temporaryDirectory: Path? = null,
) : VacancyExporter {
    private val files = DesktopJsonExport(picker, clock, temporaryDirectory)

    override suspend fun export(vacancy: Vacancy): DocumentExportResult =
        files.export("vacancy", vacancy.id) { VacancyExportJson.encode(vacancy) }
}
