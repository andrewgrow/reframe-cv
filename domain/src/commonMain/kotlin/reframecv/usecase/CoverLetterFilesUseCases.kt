package reframecv.usecase

import reframecv.domain.models.coverletter.CoverLetter
import reframecv.export.CoverLetterExporter
import reframecv.importing.CoverLetterImporter

/** Provides letter document exchange independently of platform file handling. */
class CoverLetterFilesUseCases(
    private val importer: CoverLetterImporter,
    private val exporter: CoverLetterExporter,
    private val pdfExporter: CoverLetterExporter,
) {
    suspend fun importDocument() = importer.pickAndImport()
    suspend fun generatePdf(letter: CoverLetter) = pdfExporter.export(letter)
    suspend fun export(letter: CoverLetter) = exporter.export(letter)
}
