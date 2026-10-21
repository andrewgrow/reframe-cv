package reframecv.export

import reframecv.domain.models.coverletter.CoverLetter

/** Lets cover-letter features export portable documents through platform-specific file handling. */
fun interface CoverLetterExporter {
    suspend fun export(coverLetter: CoverLetter): DocumentExportResult
}
