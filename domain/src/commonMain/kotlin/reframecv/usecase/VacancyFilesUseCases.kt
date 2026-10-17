package reframecv.usecase

import reframecv.attachments.AttachmentPicker
import reframecv.domain.models.vacancy.Vacancy
import reframecv.export.VacancyExporter
import reframecv.importing.VacancyImporter

/** Provides vacancy document exchange and attachment selection without platform APIs. */
class VacancyFilesUseCases(
    private val importer: VacancyImporter,
    private val exporter: VacancyExporter,
    private val picker: AttachmentPicker,
) {
    suspend fun importDocument() = importer.pickAndImport()
    suspend fun export(vacancy: Vacancy) = exporter.export(vacancy)
    suspend fun chooseAttachment() = picker.pick()
}
